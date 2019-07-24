package tk.valoeghese.loader;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import tk.valoeghese.loader.api.Addon;
import tk.valoeghese.loader.api.AddonEvents;
import tk.valoeghese.loader.api.AddonEvents.BuiltEventType;
import tk.valoeghese.loader.api.EventType;
import tk.valoeghese.loader.utils.FunctionalUtils;
import tk.valoeghese.loader.utils.Wrapper;
import tk.valoeghese.log.Logger;

public class Loader {
	private final Logger log;
	private final File addonsFolder;
	private static final Gson gson = new Gson();
	private LocalJsonAddonWrapper localEntryPoints;

	private final Map<String, JarLoader> addons = new HashMap<>();
	private final Map<String, EventType> events = new HashMap<>();
	private final List<Object> entrypointObjects = new ArrayList<>();
	private final Map<String, List<Object>> eventSubscribers = new HashMap<>();
	private final List<Addon> addonAnnotations = new ArrayList<>();

	private static Loader recentInstance = null;

	public static Loader getMostRecentInstance() {
		return recentInstance;
	}

	public Loader() {
		log = new Logger("ValLoader").setDebug(true);
		log.debug("Created Loader Successfully.");

		addonsFolder = new File("./addons");
		if (!addonsFolder.mkdirs()) {
			log.debug("Addons folder could not be created. This is likely because it already exists.");
		} else {
			log.debug("Created addons folder.");
		}

		recentInstance = this;
	}

	public void registerEvent(EventType event) {
		events.put(event.getIdentifier(), event);
	}

	public boolean eventExists(String eventName) {
		return events.containsKey(eventName);
	}

	public boolean eventHasConfirmedSubscribers(String eventName) {
		return eventSubscribers.containsKey(eventName);
	}

	/**
	 * @return null if the EventType is not a BuiltEventType, otherwise the BuiltEventType
	 * @throws NullPointerException if the EventType does not currently exist
	 */
	public BuiltEventType getBuilderEvent(String name) throws NullPointerException {
		EventType result =  events.get(name);
		
		if (result instanceof BuiltEventType) {
			return (BuiltEventType) result;
		} else {
			return null;
		}
	}
	
	public EventType getEventType(String name) throws NullPointerException {
		return events.get(name);
	}

	public void postEvent(AddonEvents event) {
		String eventName = event.getIdKey();
		if (events.containsKey(eventName)) { // does event exist and is it registered
			entrypointObjects.forEach(obj -> {
				try {
					Method m = obj.getClass().getMethod(eventName, event.getClass());
					m.invoke(obj, event);

					eventSubscribers.computeIfAbsent(eventName, name -> new ArrayList<>()).add(obj); // add to subs for event if successful
				} catch (NoSuchMethodException e) {

				} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					log.debug("Exception in executing an event:");
					if (log.isDebug()) {
						e.printStackTrace();
					}
				}
			});
		}
	}
	
	/**
	 * Post event to confirmed subscribers thereof.<br/>An event has confirmed subscribers if it has been posted before via the postEvent() method.
	 */
	public <T extends AddonEvents> void postEventToConfirmedSubscribers(T event) {
		String eventName = event.getIdKey();
		
		if (eventSubscribers.containsKey(eventName)) { // does event have confirmed subscribers
			eventSubscribers.get(eventName).forEach(obj -> {
				try {
					Method m = obj.getClass().getMethod(eventName, event.getClass());
					m.invoke(obj, event);
				} catch (NoSuchMethodException e) {
					log.warn("A confirmed subscriber does not have event method: " + eventName);
				} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					log.debug("Exception in executing an event:");
					if (log.isDebug()) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	Loader loadAllExternalAddons() {
		File[] jars = this.findAllJars();
		FunctionalUtils.forEachItem(jars, file -> {
			try {
				JarLoader loader = JarLoader.loadJar(file.toURI().toURL());
				String jarJson = loader.setDataJsonLoc(addonsFolder.getAbsolutePath() + "/" + file.getName() + ".json");
				if (loadAddonsFor(jarJson, loader)) {
					log.debug("Initialized addon from json: addons/" + new File(jarJson).getName());
				}
			} catch (MalformedURLException e) {
				log.debugAll("MalformedUrl: ", e.toString(), " , Skipping file");
			}
		});

		return this;
	}

	Loader postInitialize() {
		entrypointObjects.forEach(obj -> {
			try {
				Method m = obj.getClass().getMethod("onPostInit", AddonEvents.PostInit.class);
				m.invoke(obj, new AddonEvents.PostInit(getAllAddonIds()));

			} catch (NoSuchMethodException e) {
				log.debug(obj.getClass().getName() + " has no postInit method.");
			} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.debug("Exception in executing postInit:");
				if (log.isDebug()) {
					e.printStackTrace();
				}
			}
		});
		return this;
	}

	private List<String> getAllAddonIds() {
		List<String> returns = new ArrayList<>();
		addonAnnotations.forEach(annotation -> returns.add(annotation.id()));
		return returns;
	}

	private File[] findAllJars() {
		return addonsFolder.listFiles(new FilenameFilter() {
			@Override public boolean accept(File dir, String name) {
				return name.endsWith(".jar") && !(new File(dir.getAbsolutePath() + "/" + name).isDirectory());
			}
		});
	}

	private boolean loadAddonsFor(String jsonFile, JarLoader jarLoader) {
		final Wrapper<Boolean> isSuccessful = Wrapper.wrap(false); // Lambda needs this to be final in an enclosing scope. Thus we use a wrapper with getters/setters.

		try (FileReader reader = new FileReader(jsonFile))
		{
			if (jarLoader == null) {
				localEntryPoints = gson.fromJson(reader, LocalJsonAddonWrapper.class);
				FunctionalUtils.forEachItem(localEntryPoints.entrypoints, item -> this.initializeAddon(item, isSuccessful, jarLoader, jsonFile));
			} else {
				ExternalJsonAddonWrapper dataJson = gson.fromJson(reader, ExternalJsonAddonWrapper.class);
				FunctionalUtils.accept(dataJson.entrypoint, item -> this.initializeAddon(item, isSuccessful, jarLoader, jsonFile));
				if (isSuccessful.getT()) { // if this is an addon
					log.debug("Setting data json wrapper for the current external addon.");
					jarLoader.setDataJson(dataJson);
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return isSuccessful.getT();
	}

	private void initializeAddon(String item, Wrapper<Boolean> resultWrapper, JarLoader jarLoader, String jsonFile) {
		log.debug("Attempting to load addon entry " + item);
		try {
			Class<?> clazz = jarLoader == null ? Class.forName(item) : jarLoader.loadClass(item);
			if (clazz.isAnnotationPresent(Addon.class)) {
				FunctionalUtils.accept(clazz.getAnnotation(Addon.class), annotation -> {
					log.infoAll("Loading specified addon ", annotation.name(), "-", annotation.version(), " [id: ", annotation.id(), "]");
					addonAnnotations.add(annotation);
					if (jarLoader != null) { // JarLoader is not null, add jarLoader to addons map
						log.debug("Adding JarLoader to addon map");
						addons.put(annotation.id(), jarLoader);
					} else {
						log.debug("Note: the detected addon is local (within-jar)");
					}
				});
				resultWrapper.setT(true); // Loader jar contains an addon, will return true
				try {
					Object newClazzInstance = clazz.newInstance();
					entrypointObjects.add(newClazzInstance); // Add loaded class instance to the list of entrypointObjects

					Method m = clazz.getMethod("onInit", AddonEvents.Init.class);
					FunctionalUtils.accept(clazz.getAnnotation(Addon.class), annotation -> {
						log.debugAll("Initializing addon ", annotation.name());
					});

					m.invoke(newClazzInstance, new AddonEvents.Init(clazz.getAnnotation(Addon.class).name(), this));
				} catch (NoSuchMethodException e) {
					log.debug("No init method found");
					e.printStackTrace();
				} catch (Exception e) {
					log.warn("This is not good.");
					e.printStackTrace();
				}
			} else {
				log.debug("Class is not an addon. Aborting.");
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	Loader loadLocalAddons() {
		this.loadAddonsFor(addonsFolder.getAbsolutePath() + "/_loader-local.json", null);
		return this;
	}

	public static class LocalJsonAddonWrapper {
		public String[] entrypoints;
	}

	public static class ExternalJsonAddonWrapper {
		public String entrypoint;
	}
}
