package org.jenkinsci.plugins.vaddy;

import java.net.*;

import net.jumperz.net.MRequestUri;
import net.jumperz.util.MThreadPool;
import vaddy.MVaddyUtil;
import vaddy.mock.MWebServer;

public class Test
{

static MThreadPool tp = new MThreadPool( 3 );

//--------------------------------------------------------------------------------
public static void main( String[] args )
throws Exception
{
try
	{
	/*
	test1();
	test2();
	 */
	test3();	
	p( "==== OK ====" );
	}
finally
	{
	tp.stop();
	}

}
//--------------------------------------------------------------------------------
public static void test3()
throws Exception
{
MWebServer webServer = new MWebServer();
tp.addCommand( webServer );
webServer.putResponse( "/v1/scan", "{'scan_id':12345678}" );
webServer.putResponse( "/v1/scan/result", "{ 'status':'finish', 'fqdn' : 'www.example.com', 'scan_id' : '1-837b5f9f-e088-4af5-9491-67f7ce8035a4', 'scan_count' : 22, 'alert_count' : 0, 'timezone' : 'UTC', 'start_time' :  '2014-08-22T15:05:06Z', 'end_time' :  '2014-08-22T15:05:06Z', 'scan_result_url' : 'https://console.vaddy.net/scan_status/1/1-837b5f9f-e088-4af5-9491-67f7ce8035a4' }" );

MPluginImpl pi = new MPluginImpl( "www.example.jp", "Kanatoko", "12345", "https://api.vaddy.net/", "127.0.0.1", "8079" );
Socket socket = MVaddyUtil.connect( "127.0.0.1", 20080, 3000 );
String scanId = pi.startScan( "dummy", socket );
if( !scanId.equals( "12345678" ) )
	{
	ex();
	}

socket = MVaddyUtil.connect( "127.0.0.1", 20080, 3000 );
if( !pi.getResult( new MRequestUri( "http://127.0.0.1:20080/" ), scanId ) )
	{
	ex();
	}
}
//--------------------------------------------------------------------------------
public static void test1()
throws Exception
{
MPluginImpl pi = new MPluginImpl( "www.example.jp", "Kanatoko", "12345", "https://api.vaddy.net/", "127.0.0.1", "8079" );
pi.execute();
}
//--------------------------------------------------------------------------------
public static void test2()
throws Exception
{
MPluginImpl pi = new MPluginImpl( "www.example.jp", "Kanatoko", "12345", "https://api.vaddy.net/", "", "8079" );
pi.execute();
}
//--------------------------------------------------------------------------------
public static void p( Object o )
{
System.out.println( o );
}
//--------------------------------------------------------------------------------
public static void ex()
throws Exception
{
throw new Exception();
}
//--------------------------------------------------------------------------------
}