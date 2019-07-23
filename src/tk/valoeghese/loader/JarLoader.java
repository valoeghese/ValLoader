package tk.valoeghese.loader;

import java.net.URL;
import java.net.URLClassLoader;

import tk.valoeghese.loader.Loader.ExternalJsonAddonWrapper;

public final class JarLoader {
	private final URLClassLoader loader;
	private String dataJsonLoc = null;
	private ExternalJsonAddonWrapper dataJson;
	
	private JarLoader(URLClassLoader loader) throws NullPointerException {
		if (loader == null) {
			throw new NullPointerException("Loader is null! Likely a malformed URL?");
		}
		this.loader = loader;
	}

	public static JarLoader loadJar(URL jar) {
		URLClassLoader loader;
		loader = new URLClassLoader(new URL[] {
				jar
		});

		return new JarLoader(loader);
	}
	
	public String setDataJsonLoc(String loc) {
		this.dataJsonLoc = loc;
		return loc;
	}
	public String getDataJsonLoc() {
		return dataJsonLoc;
	}
	
	public ExternalJsonAddonWrapper setDataJson(ExternalJsonAddonWrapper data) {
		this.dataJson = data;
		return data;
	}
	public ExternalJsonAddonWrapper getDataJson() {
		return dataJson;
	}

	public Class<?> loadClass(String name) {
		try {
			return loader.loadClass(name);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
}
