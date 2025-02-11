package com.github.kozakura913.fedstorage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import net.arnx.jsonic.JSON;

public class HttpServer extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final HashSet<String> files = new HashSet<>();

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		String path = request.getPathInfo();

		if (path.startsWith("/api")) {
			response.setHeader("Content-Type","application/json");
			PrintWriter w = response.getWriter();
			if (path.equals("/api/list/item_frequency.json")) {
				item_frequency(w);
			} else if (path.equals("/api/list/items.json")) {
				items(w,request);
			} else if (path.equals("/api/list/fluid_frequency.json")) {
				fluid_frequency(w);
			} else if (path.equals("/api/list/fluids.json")) {
				fluids(w,request);
			} else if (path.equals("/api/list/energy_frequency.json")) {
				energy_frequency(w);
			}
			w.flush();
			return;
		}

		if (path.endsWith(".html")) {
			response.setHeader("Content-Type","text/html");
		} else if (path.endsWith(".css")) {
			response.setHeader("Content-Type","text/css");
		} else if (path.endsWith(".js")) {
			response.setHeader("Content-Type","text/javascript");
		} else if (path.endsWith(".png")) {
			response.setHeader("Content-Type","image/png");
		} else if (path.endsWith(".svg")) {
			response.setHeader("Content-Type","image/svg+xml");
		}

		path = path.substring(1);

		if (path.contains("/") || path.startsWith(".")) {
			response.setStatus(404);
		}

		if (path.isBlank())
			path = "index.html";

		if (files.contains(path)) {
			URL res = HttpServer.class.getClassLoader().getResource("com/github/kozakura913/fedstorage/html/" + path);

			if (res == null) {
				response.setStatus(404);
			} else {
				try(InputStream is = res.openStream()){
					is.transferTo(response.getOutputStream());
				}
			}
		} else {
			response.setStatus(404);
		}
	}

	private void fluids(PrintWriter w, HttpServletRequest req) {
		String freq = req.getParameter("frequency");

		if (freq == null) {
			w.append("[]");
			return;
		}

		freq = freq.toUpperCase();
		HashMap<String, FluidStack> freq_buffer = FedStorageServer.fluid_buffers.get(freq);

		if (freq_buffer == null) {
			w.append("[]");
			return;
		}

		ArrayList<FluidStack> copy = new ArrayList<>();

		synchronized(freq_buffer) {
			copy.addAll(freq_buffer.values());
		}

		ArrayList<Fluids> result = new ArrayList<>();
		for(FluidStack fs : copy) {
			StringBuffer nbt = new StringBuffer();
			Fluids fluids_rec = new Fluids();

			if (fs.nbt == null) {
				nbt.append("null");
			} else {
				nbt.append("\"");

				for (int j = 0; j < fs.nbt.length; j++){
					int i = (int)fs.nbt[j] & 0xff;
					if (i <= 15){
						nbt.append("0");
					}
					nbt.append(Integer.toHexString(i));
				}
				nbt.append("\"");
			}

			fluids_rec.setNbt(nbt.toString());
			fluids_rec.setCount(fs.count);
			fluids_rec.setName(fs.name);
			result.add(fluids_rec);			
		}
		w.append(JSON.encode(result));
	}

	private void energy_frequency(Writer w) throws IOException {
		class FreqEntry{
			String id;
			long value;
		}

		ArrayList<FreqEntry> copy = new ArrayList<>();
		synchronized(FedStorageServer.energy_buffers) {
			for(Entry<String, EnergyStack> e : FedStorageServer.energy_buffers.entrySet()) {
				FreqEntry fe = new FreqEntry();
				fe.id = e.getKey();
				fe.value = e.getValue().value;
				copy.add(fe);
			}
		}
		w.append("[\n");

		boolean first = true;
		for(FreqEntry fe : copy) {
			if (!first) {
				w.append(",\n");
			} else {
				first = false;
			}
			w.append("{\"id\":\"").append(fe.id).append("\",\"value\":").append(Long.toString(fe.value)).append("}");
		}
		w.append("\n]");
	}

	private void fluid_frequency(Writer w) throws IOException {
		ArrayList<FluidFrequency> result = new ArrayList<>();

		synchronized(FedStorageServer.fluid_buffers) {
			for(Entry<String, HashMap<String, FluidStack>> e : FedStorageServer.fluid_buffers.entrySet()) {
				FluidFrequency fe = new FluidFrequency();
				fe.setId(e.getKey());
				fe.setSize(e.getValue().size());
				result.add(fe);
			}
		}
		w.append("[\n");

		boolean first = true;
		for(FreqEntry fe : copy) {
			if (!first) {
				w.append(",\n");
			} else {
				first = false;
			}
			w.append("{\"id\":\"").append(fe.id).append("\",\"size\":").append(Integer.toString(fe.size)).append("}");
		}
		w.append("\n]");
	}

	private void item_frequency(Writer w) throws IOException {
		ArrayList<ItemFrequency> result = new ArrayList<>();
		synchronized(FedStorageServer.item_buffers) {
			for(Entry<String, ArrayList<ItemStack>> e : FedStorageServer.item_buffers.entrySet()) {
				ItemFrequency fe = new ItemFrequency();
				fe.setId(e.getKey());
				fe.setSize(e.getValue().size());
				result.add(fe);
			}
		}
		w.append(JSON.encode(result));
	}

	private void items(Writer w,HttpServletRequest req) throws IOException {
		String freq = req.getParameter("frequency");

		if (freq == null) {
			w.append("[]");
			return;
		}

		freq = freq.toUpperCase();
		ArrayList<ItemStack> freq_buffer = FedStorageServer.item_buffers.get(freq);
		if (freq_buffer == null) {
			w.append("[]");
			return;
		}

		ArrayList<ItemStack> copy;
		synchronized(freq_buffer) {
			copy=(ArrayList<ItemStack>) freq_buffer.clone();
		}

		ArrayList<Items> result = new ArrayList<>();
		for(ItemStack is : copy) {
			Items items_rec = new Items();
			StringBuffer nbt = new StringBuffer();

			if (is.nbt == null) {
				nbt.append("null");
			} else {
				nbt.append("\"");
				for (int j = 0; j < is.nbt.length; j++){
					int i = (int)is.nbt[j] & 0xff;
					if (i <= 15){
						nbt.append("0");
					}
					nbt.append(Integer.toHexString(i));
				}
				nbt.append("\"");
			}

			items_rec.setNbt(nbt.toString());
			items_rec.setCount(is.count);
			items_rec.setName(is.name);

			result.add(items_rec);
		}
		w.append(JSON.encode(result));
	}

	public static void start() {
		String[] src = new File("src/com/github/kozakura913/fedstorage/html").list();
		if (src != null && src.length > 0) {
			try (FileOutputStream list_txt = new FileOutputStream("src/com/github/kozakura913/fedstorage/html/list.txt")){
				for(String file : src) {
					list_txt.write((file + "\n").getBytes("UTF-8"));
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		URL res = HttpServer.class.getClassLoader().getResource("com/github/kozakura913/fedstorage/html/list.txt");
		if (res == null) {
			System.out.println("FrontendResource notfound");
		} else {
			try(InputStream is = res.openStream()){
				BufferedReader br = new BufferedReader(new InputStreamReader(is));

				while(true) {
					String line = br.readLine();
					if (line == null)
						break;
					files.add(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Server server = new Server(3031);
		ServletContextHandler context = new ServletContextHandler("/",ServletContextHandler.SESSIONS);

		context.setWelcomeFiles(new String[] { "index.html" });
		context.addServlet( new ServletHolder( new HttpServer()), "/*");
		server.setHandler(context);

		try {
			server.start();
			System.out.println("ServerStart");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
