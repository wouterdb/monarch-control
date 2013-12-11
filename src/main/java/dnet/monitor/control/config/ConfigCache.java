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
package dnet.monitor.control.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import com.google.gson.Gson;

import dnet.monitor.control.Command;
import dnet.monitor.control.amqp.CommandServer;

public class ConfigCache extends ConfigClient {

	Gson g = new Gson();

	@Override
	public void handle(Command command, CommandServer server) {
		if (command.getCommand().equals("init"))
			sendInit(command, server);
		else
			super.handle(command, server);

	}

	private Map<UUID, Command> commands = new HashMap<UUID, Command>();

	private synchronized void sendInit(Command command, CommandServer server) {
		try {

			for (Command c : commands.values()) {
				server.reply(command, g.toJson(c));
			}
		} catch (IOException e) {
			log.log(Level.WARNING,"could not resend command",e);
		}

	}

	@Override
	public synchronized void configRemoved(UUID id, Map<String, Object> cfg) {
		commands.remove(id);
	}

	@Override
	public synchronized void configUpdated(Command command) {
		commands.put(command.getId(), command);
	}

	@Override
	public synchronized void configAdded(Command command) {
		commands.put(command.getId(), command);
	}

	@Override
	public String getDeepHelp() {
		return "cache server -- no help";
	}
	
	protected void deepHelp(Command command, CommandServer server) {
	
	}

}
