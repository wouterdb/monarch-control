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
package dnet.monitor.control.query;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import dnet.monitor.control.Command;
import dnet.monitor.control.amqp.CommandServer;

public abstract class QVTDQueryTerm implements QueryTerminator {

	@Override
	public String prefix() {
		return "query";
	}

	@Override
	public void handle(Command command, CommandServer server) {
		if(command.getCommand().equals("query.do")){
			doQ(command,server);
		}
	}

	private void doQ(Command command, CommandServer server) {
		String url = (String) command.getConfig().get("url");
		
		if(url==null || !url.startsWith("qvtd://"))
			return;
		
		String[] parts = url.split("/",5);
		
		if(parts.length<4){
			server.err(command, 400, "url too short");
			return ;
		}
		
		String model = parts[2];
		String type = parts[3];
		String rel = parts.length==5?parts[4]:null;
		
		respond(command,server,model,type,rel);
	}
	

	protected void reply(Command command, CommandServer server, String model,
			String type, String jsonForm) {
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("model", model);
		headers.put("type", type);
		try {
			server.reply(command, jsonForm,headers);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	protected abstract void respond(Command command, CommandServer server, String model,
			String type, String rel) ;
}
