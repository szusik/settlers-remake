/*******************************************************************************
 * Copyright (c) 2020
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.main.swing.settings;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;

import jsettlers.common.resources.ResourceManager;

public class ServerManager {
	private static final String SERVER_FILE = "servers.json";

	private static final Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	private static ServerManager instance;

	public static ServerManager getInstance() {
		if(instance == null) {
			try(Reader serverFile = new InputStreamReader(ResourceManager.getResourcesFileStream(SERVER_FILE))) {
				instance = gson.fromJson(serverFile, ServerManager.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(instance == null) instance = new ServerManager();
			for (ServerEntry server : instance.servers) {
				server.connect();
			}
		}

		return instance;
	}


	public ServerManager() {
		servers = new ArrayList<>();
	}

	private List<ServerEntry> servers;

	public void addServer(ServerEntry server) {
		servers.add(server);
		server.connect();
		updateFile();
	}

	public void removeServer(ServerEntry server) {
		if(servers.remove(server)) {
			server.disconnect();
			updateFile();
		}
	}

	public void updateFile() {
		String newContent = gson.toJson(this);
		try(Writer serverFile = new OutputStreamWriter(ResourceManager.writeConfigurationFile(SERVER_FILE))) {
			serverFile.write(newContent);
			serverFile.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ListModel<ServerEntry> createListModel() {
		return new ServerListModel();
	}

	private class ServerListModel extends AbstractListModel<ServerEntry> {

		@Override
		public int getSize() {
			return servers.size();
		}

		@Override
		public ServerEntry getElementAt(int i) {
			return servers.get(i);
		}
	}
}
