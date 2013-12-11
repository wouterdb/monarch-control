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

import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.google.gson.Gson;

public class CommandTest {

	@Test
	public void test() {
		//conversion loses miliseconds,...
		Date then = new Date(1366035206000L);
		Map<String,Object> config = new HashMap<String, Object>();
		config.put("test", "test");
		Map<String,Object> config2 = new HashMap<String, Object>();
		config2.put("xx", "xxxx");
		config.put("test2", config2);
		Command c = new Command(UUID.randomUUID(), "True","a",then,config);
		Gson g = new Gson();
		String out = g.toJson(c);
		Command outc = g.fromJson(out, Command.class);
		assertEquals(c.getId(), outc.getId());
		//System.out.println(c.getEndTime().getTime()+" " + outc.getEndTime().getTime());
		assertEquals(c.getEndTime(), outc.getEndTime());
		assertEquals(c.getFilter(), outc.getFilter());
		assertEquals(c.getConfig(), outc.getConfig());
		assertEquals(c.getCommand(), outc.getCommand());
	}

}
