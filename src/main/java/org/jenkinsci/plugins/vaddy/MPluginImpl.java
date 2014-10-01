package org.jenkinsci.plugins.vaddy;

import java.io.*;
import java.net.Socket;
import java.util.*;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import vaddy.MConfig;
import vaddy.MConstants;
import vaddy.MVaddyUtil;
import net.jumperz.net.*;
import net.jumperz.security.MSecurityUtil;
import net.jumperz.util.MStreamUtil;
import net.jumperz.util.MSystemUtil;
import net.sf.json.JSONObject;

public class MPluginImpl
{
private final String host;
private final String userId;
private final String authKey;
private final String apiServerUrl;
private final String proxyHost;
private final String proxyPort;

private String errorMessage = "";
private PrintStream logStream = System.out;

static
{
MVaddyUtil.setConfig( new MConfig() );
}
//--------------------------------------------------------------------------------
public MPluginImpl( String host, String userId, String authKey, String apiServerUrl, String proxyHost, String proxyPort )
{
this.host = host;
this.userId = userId;
this.authKey = authKey;
this.apiServerUrl = apiServerUrl;
this.proxyHost = proxyHost;
this.proxyPort = proxyPort;
}
//--------------------------------------------------------------------------------
public void setLogStream( final PrintStream out )
{
logStream = out;
}
//--------------------------------------------------------------------------------
public String getErrorMessage()
{
return errorMessage;
}
//--------------------------------------------------------------------------------
public boolean execute()
{
try
	{
	return execute2();
	}
catch( Exception e )
	{
	e.printStackTrace( logStream );
	return false;
	}
}
//--------------------------------------------------------------------------------
public String startScan( final String apiServerHost, final Socket socket )
throws Exception
{	//スキャン開始
String scanId = "";

final Map parameterMap = new HashMap();
parameterMap.put( "action", "start" );
parameterMap.put( "user", userId );
parameterMap.put( "fqdn", host );
parameterMap.put( "auth_key", authKey );

final MHttpRequest apiRequest = MVaddyUtil.getAPIRequest( "/v1/scan", MConstants.HTTP_METHOD_POST, parameterMap );
apiRequest.setHeaderValue( "Host", apiServerHost );
apiRequest.removeHeaderValue( "X-Auth-Token" );
//log( apiRequest );
socket.getOutputStream().write( apiRequest.toByteArray() );

final MHttpResponse response = new MHttpResponse( new BufferedInputStream( socket.getInputStream() ) );
log( response );

if( response.getStatusCode() != 200 )
	{
		//エラー
	errorMessage = response.toString();
	throw new Exception( "Invalid Status Code:" + response.getStatusCode() );
	}

final Map apiResult = JSONObject.fromObject( response.getBodyAsString() );
scanId = apiResult.get( "scan_id" ) + ""; // convert int to string
return scanId;
}
//--------------------------------------------------------------------------------
public boolean getResult( final MRequestUri uri, final String scanId )
throws Exception
{
log( "Sleeping 15 seconds ..." );
Thread.sleep( 15 * 1000 ); //15秒スリープ

final long timeout = System.currentTimeMillis() + ( 1000L * 60 * 60 ); // 1hour from now

	//結果取得
while( System.currentTimeMillis() < timeout
    && Thread.currentThread().isInterrupted() == false
     )
	{
	final Map parameterMap = new HashMap();
	parameterMap.put( "user", userId );
	parameterMap.put( "fqdn", host );
	parameterMap.put( "auth_key", authKey );
	parameterMap.put( "scan_id", scanId );
	
	final MHttpRequest apiRequest = MVaddyUtil.getAPIRequest( "/v1/scan/result", MConstants.HTTP_METHOD_GET, parameterMap );
	apiRequest.setHeaderValue( "Host", uri.getHost() );
	apiRequest.removeHeaderValue( "X-Auth-Token" );
	
	MHttpResponse response = null;
	
	Socket socket = getSocket( uri );
	try
		{
		socket.getOutputStream().write( apiRequest.toByteArray() );
		response = new MHttpResponse( new BufferedInputStream( socket.getInputStream() ) );
		}
	finally
		{
		MSystemUtil.closeSocket( socket );
		}
	
	log( response );
	
	if( response.getStatusCode() != 200 )
		{
			//エラー
		errorMessage = response.toString();
		throw new Exception( "Invalid Status Code:" + response.getStatusCode() );
		}
	
	final Map apiResult = JSONObject.fromObject( response.getBodyAsString() );
	//p( apiResult );
	
	final String status = ( String )apiResult.get( "status" );
	if( status.equals( "scanning" ) )
		{
		log( "Status is 'scanning'." );
		log( "Sleeping 30 seconds ..." );
		Thread.sleep( 30 * 1000 ); //30秒スリープ
		}
	else if( status.equals( "canceled" ) )
		{
		return false;
		}
	else if( status.equals( "finish" ) )
		{
		if( ( "" + apiResult.get( "alert_count" ) ).equals( "0" ) )
			{
			log( "==== No vulnerabilities found. OK. ====" );
			return true;
			}
		else
			{
			log( "* * * * VAddy FAILURE : Some vulnerabilities found. * * * *" );
			return false;
			}
		}
	}

log( "* * * * VAddy FAILURE : Timed out or the job is canceled. * * * *" );
return false;
}
//--------------------------------------------------------------------------------
public boolean execute2()
throws Exception
{
final MRequestUri uri = new MRequestUri( apiServerUrl );
String scanId;
Socket socket = null;
try
	{
	socket = getSocket( uri );
	scanId = startScan( uri.getHost(), socket );
	}
finally
	{
	MSystemUtil.closeSocket( socket );
	}

return getResult( uri, scanId );
}
//--------------------------------------------------------------------------------
public Socket getSocket( final MRequestUri uri )
throws IOException
{
if( uri.getPort() == 80 || uri.toString().startsWith( "http:" ) )
	{
	return new Socket( uri.getHost(), uri.getPort() );
	}

boolean useProxy = false;
if( proxyHost != null
 && proxyHost.length() > 6
 && proxyPort != null
 && proxyPort.matches( "^[0-9]+$" )
  )
	{
	useProxy = true;
	}

Socket socket;
if( useProxy )
	{
	Socket proxyRawSocket = MVaddyUtil.connect( proxyHost, Integer.parseInt( proxyPort ), vaddy.MConstants.SOCKET_CONNECT_TIMEOUT_MILLI );
	final MHttpRequest connectRequest = new MHttpRequest( "CONNECT " + uri.getHost() + ":" + uri.getPort() + " HTTP/1.1\r\nHost: " + uri.getHost() + "\r\n\r\n" );
	
	final OutputStream out = proxyRawSocket.getOutputStream();
	final InputStream in = proxyRawSocket.getInputStream();
	out.write( connectRequest.toByteArray() );
	out.flush();
	
	final MHttpResponse connectResponse = new MHttpResponse( new BufferedInputStream( in ), true );
	
	socket = ( ( SSLSocketFactory )MSecurityUtil.getBogusSslSocketFactory() ).createSocket( proxyRawSocket, proxyRawSocket.getInetAddress().getHostName(), proxyRawSocket.getPort(), true );
	}
else
	{
	socket = MVaddyUtil.sslConnect( uri.getHost(), uri.getPort(), vaddy.MConstants.SOCKET_CONNECT_TIMEOUT_MILLI );
	}
return socket;
}
//--------------------------------------------------------------------------------
private void log( Object o )
{
logStream.println( o );
}
//--------------------------------------------------------------------------------
}
