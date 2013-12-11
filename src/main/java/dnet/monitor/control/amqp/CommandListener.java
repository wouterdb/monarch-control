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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;

import dnet.monitor.control.Command;


public class CommandListener implements Consumer  {
	
	
	
	private Gson g = new Gson();
	private Map<String, String> metadata;
	private CommandServer server;

	public CommandListener(CommandServer commandServer,
			Map<String, String> metadata) {
		this.server = commandServer;
		this.metadata = metadata;
	}

	public void handleCancel(String arg0) throws IOException {
	}

	public void handleCancelOk(String arg0) {	
	}

	public void handleConsumeOk(String arg0) {
	}

	public void handleDelivery(String consumerTag, Envelope envelope,
			BasicProperties properties, byte[] body) throws IOException {
		Command c = g.fromJson(new InputStreamReader(new ByteArrayInputStream(body)), Command.class);
		c.setReturnQueue(properties.getReplyTo());
		if(c.matches(metadata))
			server.handle(c);
	}

	public void handleRecoverOk(String arg0) {		
	}

	public void handleShutdownSignal(String arg0, ShutdownSignalException arg1) {
		
	}
	

	

}
