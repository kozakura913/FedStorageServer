package com.github.kozakura913.fedstorage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
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
public class HttpServer extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final HashSet<String> files = new HashSet<>();

	private static final String endpoints[][] = {
		{"/api/list/item_frequency.json", "item_frequency"},
		{"/api/list/items.json", "items"},
		{"/api/list/fluid_frequency.json", "fluid_frequency"},
		{"/api/list/fluids.json", "fluids"},
	};

	private static final String contentTypes[][] = {
		{".html", "text/html"},
		{".css",  "text/css"},
		{".js",  "text/javascript"},
		{".png", "text/png"},
		{".svg", "text/svg+xml"},
	};

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		String path = request.getPathInfo();

		if (path.startsWith("/api")) {
			response.setHeader("Content-Type","application/json");
			PrintWriter w = response.getWriter();
			Method method;

			for (String[] endpoint : endpoints) {
				if (path.equals(endpoint[0])) {
					method = this.getClass().getMethod(endpoint[1], new Class[]{Writer.class, HttpServletRequest.class});
					method.invoke(w, request);
					break;
				}
			}

			w.flush();
			return;
		}

		for (String[] contentType : contentTypes) {
			if (path.endsWith(contentType[0])) {
				response.setHeader("Content-Type", contentType[1]);
			}
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
				try (InputStream is = res.openStream()) {
					is.transferTo(response.getOutputStream());
				}
			}
		} else {
			response.setStatus(404);
		}
	}

	@SuppressWarnings("unused")
	private void fluids(PrintWriter w, HttpServletRequest req) {
		String freq=req.getParameter("frequency");
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
		w.append("[\n");

		boolean first = true;
		for (FluidStack fs:copy) {
			if (!first) {
				w.append(",\n");
			} else {
				first = false;
			}

			StringBuffer nbt = new StringBuffer();
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
			w.append("{\"name\":\"").append(fs.name).append("\",\"count\":").append(Long.toString(fs.count)).append(",\"nbt\":").append(nbt).append("}");
		}
		w.append("\n]");
	}

	@SuppressWarnings("unused")
	private void fluid_frequency(Writer w, HttpServletRequest req) throws IOException {
		class FreqEntry{
			String id;
			int size;
		}
		ArrayList<FreqEntry> copy = new ArrayList<>();
		synchronized(FedStorageServer.fluid_buffers) {
			for(Entry<String, HashMap<String, FluidStack>> e : FedStorageServer.fluid_buffers.entrySet()) {
				FreqEntry fe = new FreqEntry();
				fe.id = e.getKey();
				fe.size = e.getValue().size();
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
			w.append("{\"id\":\"").append(fe.id).append("\",\"size\":").append(Integer.toString(fe.size)).append("}");
		}
		w.append("\n]");
	}

	@SuppressWarnings("unused")
	private void item_frequency(Writer w, HttpServletRequest req) throws IOException {
		class FreqEntry{
			String id;
			int size;
		}
		ArrayList<FreqEntry> copy = new ArrayList<>();
		synchronized(FedStorageServer.item_buffers) {
			for(Entry<String, ArrayList<ItemStack>> e:FedStorageServer.item_buffers.entrySet()) {
				FreqEntry fe = new FreqEntry();
				fe.id = e.getKey();
				fe.size = e.getValue().size();
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
			w.append("{\"id\":\"").append(fe.id).append("\",\"size\":").append(Integer.toString(fe.size)).append("}");
		}
		w.append("\n]");
	}

	@SuppressWarnings("unused")
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
			copy = new ArrayList<ItemStack>(freq_buffer);
		}
		w.append("[\n");
		boolean first = true;
		for(ItemStack is : copy) {
			if (!first) {
				w.append(",\n");
			} else {
				first = false;
			}
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
			w.append("{\"name\":\"").append(is.name).append("\",\"count\":").append(Integer.toString(is.count)).append(",\"nbt\":").append(nbt).append("}");
		}
		w.append("\n]");
	}

	public static void start() {
		String[] src = new File("src/com/github/kozakura913/fedstorage/html").list();
		if (src != null && src.length > 0) {
			try (FileOutputStream list_txt = new FileOutputStream("src/com/github/kozakura913/fedstorage/html/list.txt")){
				for(String file:src) {
					list_txt.write((file+"\n").getBytes("UTF-8"));
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
					if (line == null)break;
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
