package io.exampleaddon;

import tk.valoeghese.loader.Loader;
import tk.valoeghese.loader.api.Addon;
import tk.valoeghese.loader.api.AddonEvents;
import tk.valoeghese.loader.api.AddonEvents.BuiltEvent;
import tk.valoeghese.loader.api.AddonEvents.BuiltEventType;
import tk.valoeghese.log.Logger;

@Addon(id = "localaddon", name = "My Local Addon")
public class MyAddon{
	
	Logger log;
	
	public void onInit(AddonEvents.Init e) {
		log = e.getAddonLog();
		
		log.info("Hello, World!");
		
		e.registerEvent(e.eventBuilder("myEvent")
				.addProperty("exampleProperty", Integer.class)
				.addProperty("exampleProperty2", Integer.class)
				.addProperty("exampleProperty3", String.class)
				.build());
	}
	
	public void onPostInit(AddonEvents.PostInit e) {
		BuiltEventType b = Loader.getMostRecentInstance().getBuilderEvent("myEvent");
		BuiltEvent event = b.create(b.itemBuilder()
				.addItem("exampleProperty", 3)
				.addItem("exampleProperty2", 4)
				.addItem("exampleProperty3", "five")
				.build());
		
		log.info("Posting custom event");
		Loader.getMostRecentInstance().postEvent("myEvent", event);
	}
	
	public void myEvent(AddonEvents.BuiltEvent e) {
		log.info("Recieved custom event");
		log.info(e.getEventProperty("exampleProperty", Integer.class).toString());
		log.info(e.getEventProperty("exampleProperty3", String.class));
	}
}
