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
package dnet.monitor.control.amqp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import dnet.monitor.control.Command;

public class CommandClient {

	public class ReaperTask extends TimerTask {

		private String id;

		public ReaperTask(String id) {
			this.id = id;
		}

		@Override
		public void run() {
			CommandClient.this.remove(id);

		}

	}

	private static Logger log = Logger.getLogger(CommandClient.class.getName());

	private String replyQueue;
	private Connection connection;
	private Channel commandChannel;
	private RPCListener commandListener;

	private Map<String, dnet.monitor.control.RPCListener> listeners = new HashMap<String, dnet.monitor.control.RPCListener>();
	private Timer listenerReaper = new Timer(true);

	Gson g = new GsonBuilder().setDateFormat(DateFormat.FULL,DateFormat.FULL).create();
	

	

	public CommandClient(Connection connection) {
		this.connection = connection;
		this.commandListener = new RPCListener(this);
	}

	public void start() throws IOException {
		commandChannel = connection.createChannel();

		replyQueue = commandChannel.queueDeclare().getQueue();

		// FIXME perhaps do manual ack?
		commandChannel.basicConsume(replyQueue, true, commandListener);

	}
	
	public void stop() throws IOException {
		commandChannel.close();
	}


	public void handle(String requestId, String callerID, String messg, Map<String, Object> headers) {
		dnet.monitor.control.RPCListener listener = listeners.get(requestId);
		if (listener == null) {
			log.finest("No Listener for " + requestId);
			return;
		}
		listener.received(callerID, messg, headers);
	}
	
	public void handleErr(String requestId, String callerID, int code, String messg) {
		dnet.monitor.control.RPCListener listener = listeners.get(requestId);
		if (listener == null) {
			log.finest("No Listener for " + requestId);
			return;
		}
		listener.err(callerID, code,messg);
	}

	public void send(Command c, dnet.monitor.control.RPCListener list,
			int deadline) throws IOException {
		BasicProperties.Builder builder = new com.rabbitmq.client.AMQP.BasicProperties.Builder()
				.contentEncoding("UTF-8").correlationId(c.getId().toString());

		if (list != null) {
			add(c, list, deadline);
			builder.replyTo(replyQueue);
		}

		BasicProperties props = builder.build();
		commandChannel.basicPublish(CommandServer.exchange, CommandServer.key,
				props, g.toJson(c).getBytes("UTF-8"));
	}
	
	public String doRpc(Command c,int deadline) throws IOException, InterruptedException, RPCException{
		HangListener hl = new HangListener();
		send(c, hl, deadline);
		RPCException e = hl.failed();
		if(e!=null)
			throw e;
		return hl.result();
	}

	private synchronized void add(Command c,
			dnet.monitor.control.RPCListener list, int deadline) {
		String id = c.getId().toString();
		list.init(this);
		listeners.put(id, list);
		if (deadline > 0)
			listenerReaper.schedule(new ReaperTask(id), deadline);
	}

	public synchronized void remove(String id) {
		dnet.monitor.control.RPCListener list = listeners.remove(id);
		if(list!=null)
				list.removed();
	}

	
}
