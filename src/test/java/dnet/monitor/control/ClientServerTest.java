/**
 *
 *     Copyright 2013 KU Leuven Research and Development - iMinds - Distrinet
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 *     Administrative Contact: dnet-project-office@cs.kuleuven.be
 *     Technical Contact: wouter.deborger@cs.kuleuven.be
 */
package dnet.monitor.control;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import dnet.monitor.control.amqp.CommandClient;
import dnet.monitor.control.amqp.CommandHandler;
import dnet.monitor.control.amqp.CommandServer;

public class ClientServerTest {
	
	public static class HangListener implements RPCListener {

		private boolean called;
		private boolean failed;

		@Override
		public void init(CommandClient c) {
			
			
		}

		@Override
		public synchronized void received(String id, String s,Map<String,Object> headers) {
			called = true;
			this.notify();
			
		}

		@Override
		public void removed() {
			
			
		}
		
		private synchronized void hang() throws InterruptedException {
			if(!called)
				this.wait();

		}
		
		private synchronized boolean hang(int time) throws InterruptedException {
			if(!called)
				this.wait(time);
			return called;
		}

		@Override
		public synchronized void err(String id, int code, String messg) {
			failed = true;
			this.notify();
		}

		public synchronized boolean failed() throws InterruptedException {
			if(!failed)
				this.wait();
			return failed;
		}

	}
	
	public static class CountListener implements RPCListener {
		
		public CountListener(int wiating) {
			this.waiting = wiating;
		}

		private int waiting;
		private CommandClient client;

		@Override
		public void init(CommandClient c) {
			this.client = c;
			
			
		}

		@Override
		public synchronized void received(String id, String s,Map<String,Object> headers) {
			System.out.println(id);
			assertEquals("PONG", s);
			waiting--;
			if(waiting==0){
				notify();
			}
		}

		@Override
		public synchronized void removed() {
			notify();
			
		}
		
		public synchronized void hang() throws InterruptedException {
			if(waiting!=0)
				this.wait();

		}
		
		public synchronized int hang(int time) throws InterruptedException {
			if(waiting!=0)
				this.wait(time);
			return waiting;
		}

		@Override
		public void err(String id, int code, String messg) {
			// TODO Auto-generated method stub
			
		}

	}

	
	private static CommandServer server;
	private static CommandClient client;

	public static Connection makeConnection() throws IOException{
		ConnectionFactory c = new ConnectionFactory();
		c.setHost("localhost");
		c.setUsername("guest");
		c.setPassword("guest");
		return c.newConnection();
	}

	@BeforeClass
	public static void makeServer() throws IOException{
		Map<String, String> metadata = new HashMap<String, String>();
		metadata.put("type", "server");
		metadata.put("tier", "x.y.z");
		List<CommandHandler> handlers = new LinkedList<>();
		handlers.add(new PingHandler());
		
		server = new CommandServer("testS"+System.currentTimeMillis(), makeConnection(), metadata, handlers); 
		server.start();
	}
	
	@BeforeClass
	public static void makeClient() throws IOException{
		client = new CommandClient(makeConnection());
		client.start();
	}
	
	@Test(timeout=1000)
	public void pingTest() throws InterruptedException, IOException {
		HangListener h = new HangListener();
		client.send(new Command("true","ping", null, null), h, -1);
		h.hang();
	}
	
	@Test(timeout=1000)
	public void pingTest2() throws InterruptedException, IOException {
		HangListener h = new HangListener();
		client.send(new Command("type==\"server\"","ping", null, null), h, -1);
		h.hang();
	}
	
	
	@Test(timeout=1000)
	public void regexTest() throws InterruptedException, IOException {
		HangListener h = new HangListener();
		client.send(new Command("tier~=\"x[.]y.*\"","ping", null, null), h, -1);
		h.hang();
	}
	
	@Test()
	public void regexTest2() throws InterruptedException, IOException {
		HangListener h = new HangListener();
		client.send(new Command("tier~=\"x[.]z.*\"","ping", null, null), h, -1);
		assertTrue(!h.hang(1000));
	}
	
	@Test()
	public void regexTest3() throws InterruptedException, IOException {
		HangListener h = new HangListener();
		client.send(new Command("tier~=\"x[.]y\"","ping", null, null), h, -1);
		assertTrue(!h.hang(1000));
	}
	
	@Test()
	public void pingTest3() throws InterruptedException, IOException {
		HangListener h = new HangListener();
		client.send(new Command("type==\"foef\"","ping", null, null), h, -1);
		
		assertTrue(!h.hang(1000));
	}

	
	@Test()
	public void testRPC3() throws InterruptedException, IOException {
		HangListener h = new HangListener();
		client.send(new Command("true","blah", null, null), h, -1);
		
		assertTrue(!h.hang(1000));
	}

	@Test(timeout=1000)
	public void testRPC4() throws InterruptedException, IOException {
		HangListener h = new HangListener();
		client.send(new Command("true","blah", null, null), h, -1);
		
		assertTrue(h.failed());
	}

	
	@Test(timeout=1000)
	public void testRPC() throws IOException, InterruptedException{
		CountListener h = new CountListener(1);
		client.send(new Command("true","ping", null, null), h, -1);
		h.hang();
	}
	
	@Test(timeout=1000)
	public void testRPC2() throws IOException, InterruptedException{
		makeServer();
		CountListener h = new CountListener(2);
		client.send(new Command("true","ping", null, null), h, -1);
		h.hang();
	}

}
