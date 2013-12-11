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
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rits.cloning.Cloner;

import dnet.help.HelpBuilder;
import dnet.monitor.control.Command;
import dnet.monitor.control.amqp.CommandHandler;
import dnet.monitor.control.amqp.CommandServer;

/**
 * @author wouter
 * 
 */
public abstract class ConfigClient implements CommandHandler {

	public static final String INIT_SERVER_KEY = "configcache";
	public static final String INIT_SERVER_VALUE = "True";
	public static final String INIT_SERVER_MVEL = "configcache";
	protected CommandServer server;

	public void init(CommandServer s) throws UnsupportedEncodingException,
			IOException {
		Gson g = new Gson();

		Command c = new Command(INIT_SERVER_MVEL, "config.init", null, null);

		BasicProperties.Builder builder = new com.rabbitmq.client.AMQP.BasicProperties.Builder()
				.contentEncoding("UTF-8").correlationId(c.getId().toString());
		builder.replyTo(s.getQueue());

		BasicProperties props = builder.build();
		s.commandChannel.basicPublish(CommandServer.exchange,
				CommandServer.key, props, g.toJson(c).getBytes("UTF-8"));
		this.server = s;
	}

	public class ReaperTask extends TimerTask {

		private UUID id;

		public ReaperTask(UUID id) {
			this.id = id;
		}

		@Override
		public void run() {
			removeConfig(id);
		}

	}

	/**
	 * if the method throws an exception, failure is reported, otherwise success
	 * 
	 * @param id
	 * @param cfg
	 */
	public abstract void configRemoved(UUID id, Map<String, Object> cfg);

	public abstract void configUpdated(Command command);

	public abstract void configAdded(Command command);
	
	public abstract String getDeepHelp();

	protected Logger log = Logger.getLogger(getClass().getName());

	protected Map<UUID, Map<String, Object>> configs = new HashMap<UUID, Map<String, Object>>();
	private Timer reaper = new Timer();

	@Override
	public String prefix() {
		return "config";
	}

	@Override
	public void handle(Command command, CommandServer server) {
		if (command.getCommand().equals("config.help"))
			sendHelp(command, server);
		else if (command.getCommand().equals("config.add")) {
			addConfig(command, server);
		} else if (command.getCommand().equals("config.remove")) {
			removeConfig(command, server);
		} else if (command.getCommand().equals("config.list")) {
			listConfig(command, server);
		} else if (command.getCommand().equals("config.configHelp")) {
			deepHelp(command, server);
		}else
			server.err(command, 501, "config module could not find command: "
					+ command.getCommand());

	}

	protected void deepHelp(Command command, CommandServer server) {
		try {
			server.reply(command, getDeepHelp());
		} catch (IOException e) {
			server.err(command, 500, "could not respond");
		}
	}

	

	private void listConfig(Command command, CommandServer server) {
		Gson g = new Gson();
		try {
			String reply = g.toJson(configs);
			server.reply(command, reply);
		} catch (IOException e) {
			log.log(Level.WARNING, "could not send out config", e);

		}

	}

	private void removeConfig(Command command, CommandServer server) {
		removeConfig( command.getId());
		try {
			server.reply(command, "OK");
		} catch (IOException e) {
			log.log(Level.WARNING, "could not send out ack", e);

		}

	}
	
	private void removeConfig(UUID id) {
		Map<String, Object> cfg = configs.remove(id);
		if (cfg != null)
			configRemoved(id, cfg);
	}

	private void addConfig(Command command, CommandServer server) {
		if (configs.containsKey(command.getId())) {
			setConfig(command);
			configUpdated(command);
		} else {
			setConfig(command);
			configAdded(command);
		}
		try {
			server.reply(command, "OK");
		} catch (IOException e) {
			log.log(Level.WARNING, "could not send out ack", e);

		}

	}

	private void setConfig(Command command) {
		// clone config, parser is destructive

		configs.put(command.getId(),
				(new Cloner()).deepClone(command.getConfig()));
		if (command.getEndTime() != null)
			reaper.schedule(new ReaperTask(command.getId()),
					command.getEndTime());
	}

	private void sendHelp(Command c, CommandServer server) {
		try {
			server.reply(c, help());
		} catch (IOException e) {
			log.log(Level.WARNING, "could not send out help");
		}

	}

	@Override
	public String version() {
		return "1.0";
	}

	private String help;

	@Override
	public String help() {
		if (help == null)
			help = HelpBuilder
					.forHandler(prefix(), version())
					.add("help", "this message")
					.add("configHelp", "list valid configs")
					.add("add", "add a new config.")
					.add("remove",
							"remove the command with the corresponding ID")
					.add("list", "list all active configs").build();
		return help;
	}

}
