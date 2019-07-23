package tk.valoeghese.loader.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tk.valoeghese.loader.Loader;
import tk.valoeghese.log.Logger;

public abstract class AddonEvents {
	
	protected final String idKey;
	
	public AddonEvents(String idKey) {
		this.idKey = idKey;
	}
	public String getIdKey() {
		return idKey;
	}
	
	public static class Init extends AddonEvents {
		private Logger logger;
		private Loader loader;
		
		public Init(String addonName, Loader loader) {
			super("onInit");
			logger = new Logger(addonName);
			this.loader = loader;
		}
		
		public Logger getAddonLog() {
			return logger;
		}
		public EventPropertyBuilder eventBuilder(String methodIdentifier) {
			return new EventPropertyBuilder(methodIdentifier);
		}
		
		public void registerEvent(BuiltEventType event) throws IllegalArgumentException, NullPointerException {
			if (event == null) {
				throw new NullPointerException("Cannot register a null event!");
			}
			
			String methodId = event.getIdentifier();
			if (methodId.equals("onInit") || methodId.equals("onPostInit")) {
				throw new IllegalArgumentException("onInit/onPostInit is a reserved methodIdentifier!");
			} else {
				loader.registerEvent(event);
			}
		}
	}
	public static class PostInit extends AddonEvents {
		private final List<String> addons;
		
		public PostInit(final List<String> addons) {
			super("onPostInit");
			this.addons = addons;
		}
		
		public boolean isAddonLoaded(String addonId) {
			return addons.contains(addonId);
		}
	}
	
	
	public static final class EventPropertyBuilder {
		private final Map<String, Class<?>> properties = new HashMap<>();
		private final String methodIdentifier;
		
		private EventPropertyBuilder(String id) {
			methodIdentifier = id;
		}
		public EventPropertyBuilder addProperty(String identifier, Class<?> propertyClass) {
			properties.put(identifier, propertyClass);
			return this;
		}
		public BuiltEventType build() {
			return new BuiltEventType(methodIdentifier, properties);
		}
	}
	public static final class EventItemBuilder {
		private final Map<String, Object> items = new HashMap<>();
		private final BuiltEventType parent;
		private EventItemBuilder(BuiltEventType e) {
			parent = e;
		}
		public Map<String, Class<?>> getAllProperties() {
			return parent.properties;
		}
		public EventItemBuilder addItem(String identifier, Object item) {
			items.put(identifier, item);
			return this;
		}
		public Map<String, Object> build() {
			return items;
		}
	}
	public static final class BuiltEventType {
		private BuiltEventType(String methodIdentifier, final Map<String, Class<?>> properties) {
			identifier = methodIdentifier;
			this.properties = properties;
		}
		
		private final Map<String, Class<?>> properties;
		private final String identifier;
		
		public String getIdentifier() {
			return identifier;
		}
		public EventItemBuilder itemBuilder() {
			return new EventItemBuilder(this);
		}
		public BuiltEvent create(final Map<String, Object> items) {
			return new BuiltEvent(identifier, properties, items);
		}
	}
	public static final class BuiltEvent extends AddonEvents {
		private final Map<String, Class<?>> properties;
		private final Map<String, Object> items;
		
		private BuiltEvent(String identifier, final Map<String, Class<?>> propertyClasses, final Map<String, Object> propertyValues) {
			super(identifier);
			properties = propertyClasses;
			items = propertyValues;
		}
		
		/**
		 * @return the property if it exists and matches the class. null otherwise.
		 */
		public <T> T getEventProperty(String propertyIdentifier, Class<T> propertyClass) {
			if (properties.containsKey(propertyIdentifier) && items.containsKey(propertyIdentifier)) {
				 if (properties.get(propertyIdentifier) == propertyClass) {
					 try {
						 return propertyClass.cast(items.get(propertyIdentifier));
					 } catch (ClassCastException e) {
						 e.printStackTrace();
						 return null;
					 }
				 }
			}
			return null;
		}
	}
}
