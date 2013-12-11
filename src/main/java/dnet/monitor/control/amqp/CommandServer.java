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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.JsonElement;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import dnet.help.HelpBuilder;
import dnet.monitor.control.Command;

public class CommandServer {

	public class HelpHandler implements CommandHandler {

		@Override
		public String prefix() {
			return "help";
		}

		@Override
		public void handle(Command command, CommandServer server) {
			StringBuilder help = new StringBuilder();
			for (CommandHandler handler : handlers) {
				help.append(handler.help());
				help.append("\n");
			}
			try {
				server.reply(command, help.toString());
			} catch (IOException e) {
				server.err(command, 500, e.getMessage());
			}

		}

		@Override
		public String version() {
			return "1.0";
		}

		@Override
		public String help() {
			return HelpBuilder.forHandler(prefix(), version()).add("", "return full help").build();
		}

	}

	private static Logger log = Logger.getLogger(CommandServer.class.getName());

	private String queue;
	public static final String exchange = "command";
	public static final String key = "broadcast";
	public static final String ERR_KEY = "error";

	private Connection connection;
	public Channel commandChannel;
	private CommandListener commandListener;
	private List<CommandHandler> handlers;
	private Map<String, String> metadata;

	private String name;

	public CommandServer(String name, Connection connection,
			Map<String, String> metadata, List<CommandHandler> handlers) {
		this.name = name;
		this.connection = connection;
		this.handlers = handlers;
		this.metadata = metadata;
		this.commandListener = new CommandListener(this, metadata);
	}
	
	public void enableHelp(){
		handlers.add(new HelpHandler());
	}

	public void start() throws IOException {
		commandChannel = connection.createChannel();

		queue = commandChannel.queueDeclare().getQueue();

		commandChannel.exchangeDeclare(exchange, "topic", false);
		commandChannel.queueBind(queue, exchange, key);
		// FIXME perhaps do manual ack?
		commandChannel.basicConsume(queue, true, commandListener);

	}
	

	public void handle(Command c) {
		try {
			for (CommandHandler h : handlers) {
				if (c.getCommand().startsWith(h.prefix())) {
					h.handle(c, this);
					return;
				}
			}

			err(c, 501, "no command handler");
		} catch (Exception e) {
			err(c, 500, "handler failed " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void reply(Command c, String content) throws IOException {
		reply(c, content, null);
	}

	public void reply(Command c, String content, Map<String, Object> headers)
			throws IOException {
		if (c.getReturnQueue() == null)
			return;
		BasicProperties.Builder props = new com.rabbitmq.client.AMQP.BasicProperties.Builder()
				.contentEncoding("UTF-8").correlationId(c.getId().toString())
				.replyTo(name);
		if (headers != null)
			props.headers(headers);
		commandChannel.basicPublish("", c.getReturnQueue(), props.build(),
				content.getBytes("UTF-8"));
	}

	public void err(Command c, int i, String msg) {
		try {
			Map<String, Object> excnHeader = new HashMap<String, Object>();
			excnHeader.put(ERR_KEY, i);
			reply(c, String.format("%3d %s", i, msg), excnHeader);
		} catch (IOException e) {
			log.log(Level.WARNING, "failed to send error", e);
		}

	}

	public String getQueue() {
		return queue;
	}

	public void reply(Command c, byte[] content,
			HashMap<String, Object> headers) throws IOException {
		if (c.getReturnQueue() == null)
			return;
		BasicProperties.Builder props = new com.rabbitmq.client.AMQP.BasicProperties.Builder()
				.correlationId(c.getId().toString())
				.replyTo(name);
		if (headers != null)
			props.headers(headers);
		commandChannel.basicPublish("", c.getReturnQueue(), props.build(),
				content);
		
	}

	public Map<String, String> getMetaData() {
		return metadata;
	}

}
