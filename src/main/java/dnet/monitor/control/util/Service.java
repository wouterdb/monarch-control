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
package dnet.monitor.control.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import dnet.monitor.control.PingHandler;
import dnet.monitor.control.amqp.CommandHandler;
import dnet.monitor.control.amqp.CommandServer;


public abstract class Service {

	protected CommandServer cs;

	public Service(String configdir) throws Exception {
		Map<String, Object> myconfig = collectConfig(configdir);

		Map<String, String> tags = (Map<String, String>) myconfig.get("tags");
		
		Map<String, Object> initial = (Map<String, Object>) myconfig.get("init");

		String server = (String) myconfig.get("server");
		String name = (String) myconfig.get("name");
		
		if(server==null || name==null){
			System.out.println("no config,....");
			System.exit(-1);
		}

		System.out.println(String.format("connecting to %s as %s, with tags %s",server,name,tags));
		
		List<CommandHandler> chs = new LinkedList<>();
		chs.add(new PingHandler());
		chs.addAll(getCommandHandlers());

		ConnectionFactory c = new ConnectionFactory();
		c.setHost(server);
		c.setUsername("guest");
		c.setPassword("guest");
		c.setPort(5672);

		cs = new CommandServer(name, c.newConnection(), tags, chs);
		
	
		load(tags,initial);
		
	}

	protected abstract void load(Map<String, String> tags, Map<String, Object> initial) throws Exception;
	protected abstract List<CommandHandler> getCommandHandlers();
	
	public void start() throws IOException {
		cs.start();
	}

	public static Map<String, Object> collectConfig(String dir) throws JsonSyntaxException, JsonIOException, FileNotFoundException {
		List<Map<String, Object>> configs = new LinkedList<>();
		
		Gson g = new Gson();
		
		File[] files = (new File(dir)).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith("json");
			}
		});
		
		Arrays.sort(files);
		
		for(File f:files){
			configs.add(g.fromJson(new FileReader(f),
					HashMap.class));
		}
		
		return merge(new HashMap<String, Object>(), configs);
		
	}

	public static Map<String, Object> merge(HashMap<String, Object> out,
			List<Map<String, Object>> configs) {
		for (Map<String, Object> map : configs) {
			merge((Map)out,(Map)map);
		}
		
		return out;
	}

	public static void merge(Map<Object,Object> out,
			Map<Object,Object> newMap) {
		for (Map.Entry entry : newMap.entrySet()) {
			if(!out.containsKey(entry.getKey())){
				out.put(entry.getKey(), entry.getValue());
			}else{
				Object outSub = entry.getValue();
				Object newSub = out.get(entry.getKey());
				if(outSub instanceof Map){
					if(!(newSub instanceof Map)){
						System.out.println("type mismatch: discarding " + newSub);
					}else
						merge((Map)newSub,(Map)outSub);
				}else if (outSub instanceof List){
					if(!(newSub instanceof List)){
						System.out.println("type mismatch: discarding " + newSub);
					}else
						merge((List)newSub,(List)outSub);
				}else{
						if(!newSub.equals(outSub))
							System.out.println("non mergeable, ignoring " + newSub +" "+ outSub);
				}
			}
		}
		
	}

	public static void merge(List outSub, List newSub) {
		outSub.addAll(newSub);
	}

	
}
