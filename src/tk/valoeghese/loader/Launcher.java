package tk.valoeghese.loader;

public class Launcher {
	public static void main(String[] args) {
		new Loader().loadLocalAddons().loadAllExternalAddons().postInitialize();
	}
}
