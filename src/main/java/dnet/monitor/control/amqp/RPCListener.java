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
import java.util.Map;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;

public class RPCListener implements Consumer {

	private CommandClient client;

	public RPCListener(CommandClient client) {
		 this.client = client;
	}

	public void handleConsumeOk(String consumerTag) {

	}

	public void handleCancelOk(String consumerTag) {

	}

	public void handleCancel(String consumerTag) throws IOException {

	}

	public void handleDelivery(String arg0, Envelope arg1,
			BasicProperties props, byte[] arg3) throws IOException {
		Map<String, Object> headers = props.getHeaders();
		if(headers != null){
			Object exn = headers.get(CommandServer.ERR_KEY);
			if(exn != null){
				client.handleErr(props.getCorrelationId(), props.getReplyTo(),(int)exn, new String(arg3));
				return;
			}	
		}
		client.handle(props.getCorrelationId(), props.getReplyTo(), new String(arg3), headers);
	}

	public void handleShutdownSignal(String consumerTag,
			ShutdownSignalException sig) {
	}

	public void handleRecoverOk(String consumerTag) {

	}

}
