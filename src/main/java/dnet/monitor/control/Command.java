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

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mvel2.MVEL;

public class Command {
	
	private UUID id;
	private Date endTime;
	private String filter;
	private Map<String,Object> config;
	private String command;
	private String returnQueue;
	
	public Command(UUID id, String filter, String command,Date endTime, Map<String,Object> args) {
		super();
		this.id = id;
		this.filter = filter;
		this.endTime = endTime;
		this.config = args;
		this.command = command;
	}

	public Command( String filter, String command,Date endTime, Map<String,Object> config) {
		this(UUID.randomUUID(),filter,command,endTime,config);
	}

	public UUID getId() {
		return id;
	}

	public Date getEndTime() {
		return endTime;
	}

	public String getFilter() {
		return filter;
	}

	public Map<String,Object> getConfig() {
		return config;
	}
	
	@SuppressWarnings("unchecked")
	public boolean matches(Map<String,String> metadata){
		try{
			return MVEL.evalToBoolean(getFilter(), null,(Map<String,Object>)(Map)metadata);
		}catch(Exception e){
			Logger.getLogger(Command.class.getName()).log(Level.INFO, "bad filter",e);
			return false;
		}
	}

	public String getCommand() {
		return command;
	}

	public String getReturnQueue() {
		return returnQueue;
	}

	public void setReturnQueue(String returnQueue) {
		this.returnQueue = returnQueue;
	}

	
}
