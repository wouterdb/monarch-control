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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dnet.help.HelpBuilder;
import dnet.monitor.control.amqp.CommandClient;
import dnet.monitor.control.amqp.CommandHandler;
import dnet.monitor.control.amqp.CommandServer;

public class FactsHandler implements CommandHandler {
	
	private static Logger log = Logger.getLogger(FactsHandler.class.getName());
	private String help;
	
	@Override
	public String prefix() {
		return "facts";
	}

	@Override
	public void handle(Command command, CommandServer server) {
		Gson gs = new GsonBuilder().setPrettyPrinting().create();
		try {
			server.reply(command,gs.toJson(server.getMetaData() ));
		} catch (IOException e) {
			log.log(Level.WARNING,"could not send ping",e);
		}

	}

	@Override
	public String version() {
		return "1.0";
	}

	@Override
	public String help() {
		if(help == null)
			help = HelpBuilder.forHandler(prefix(),version()).addDefault("reply with PONG").build();
		return help;
	}

}
