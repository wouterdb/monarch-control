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
package dnet.help;

import java.util.Collections;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.TreeMap;

import dnet.monitor.control.amqp.CommandHandler;

public class HelpBuilder {

	private String prefix;
	private String version;
	private Map<String,String> commands;

	private HelpBuilder(String prefix, String version) {
		this.prefix = prefix;
		this.version = version;
		this.commands = Collections.EMPTY_MAP;
	}

	public HelpBuilder(HelpBuilder helpBuilder) {
		this.prefix = helpBuilder.prefix;
		this.version = helpBuilder.version;
		this.commands = new TreeMap<>(helpBuilder.commands);
	}
	
	public static HelpBuilder forHandler(CommandHandler ch) {
		return new HelpBuilder(ch.prefix(),ch.version());
	}

	public static HelpBuilder forHandler(String prefix, String version) {
		return new HelpBuilder(prefix,version);
	}

	public HelpBuilder addDefault(String desc) {
		HelpBuilder out = new HelpBuilder(this);
		out.commands.put("", desc);
		return out;
	}
	
	public HelpBuilder add(String name, String desc) {
		HelpBuilder out = new HelpBuilder(this);
		out.commands.put(name, desc);
		return out;
	}
	

	public String build() {
		StringBuilder sb = new StringBuilder();
		sb.append(prefix);
		sb.append(" (");
		sb.append(version);
		sb.append(")\n");
		for(Map.Entry<String, String> entry:commands.entrySet()){
			sb.append("\t");
			sb.append(entry.getKey());
			sb.append("\t");
			sb.append(entry.getValue().replaceAll("\n", ""));
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public static HelpBuilder parse(String s){
		Scanner st = new Scanner(s);
		String first = st.nextLine();
		Scanner f = new Scanner(first);
		f.useDelimiter(" ");
		String prefix = f.next();
		String version = f.next("(.*)");
		version = version.substring(1,version.length()-1);
		return new HelpBuilder(prefix,version);
	}

	public String getPrefix() {
		return prefix;
	}

	public String getVersion() {
		return version;
	}

	
	@Override
	public String toString() {
		return build();
	}
	

}
