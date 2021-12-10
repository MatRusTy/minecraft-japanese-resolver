package rusty.japaneseresolver;


import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.LiteralText;

import java.util.List;

public class JapaneseResolverClient implements ClientModInitializer {

	private static KeyBinding jotobakeyBinding;
	private static KeyBinding jishoKeyBinding;
	private JishoSearch jisho;
	private KanjiResolver kanjiRs;

	@Override
	public void onInitializeClient() {
		jisho = new JishoSearch(); // use default search limits
		kanjiRs = new KanjiResolver();

		jishoKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.japaneseresolver.resolveJisho", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.japaneseresolver.resolve"));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (jishoKeyBinding.wasPressed()) {
				String jpText = client.player.getMainHandStack().getName().getString();
				client.player.sendMessage(new LiteralText(" "), false);
				client.player.sendMessage(new LiteralText("Resolving " + jpText + "..."), false);
				new Thread(() -> {
					List<String> resolvedJapaneseParts = jisho.jishoSearch(jpText);
					for (String part : resolvedJapaneseParts) {
						client.player.sendMessage(new LiteralText(part), false);
					}
				}).start();
			}
		});

		jotobakeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.japaneseresolver.resolveKanji", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.japaneseresolver.resolve"));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (jotobakeyBinding.wasPressed()) {
				String jpText = client.player.getMainHandStack().getName().getString();
				client.player.sendMessage(new LiteralText(" "), false);
				client.player.sendMessage(new LiteralText("Resolving all kanji in " + jpText + "..."), false);
				new Thread(() -> {
					List<String> resolvedKanji = kanjiRs.resolveAllKanjiInString(jpText);
					for (String rsKanji : resolvedKanji) {
						client.player.sendMessage(new LiteralText(rsKanji), false);
					}
				}).start();
			}
		});

	}
	
}
