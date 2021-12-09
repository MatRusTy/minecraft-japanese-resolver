package rusty.japaneseresolver;


import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.LiteralText;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.*;

public class JapaneseResolverClient implements ClientModInitializer {

	private static KeyBinding keyBinding;

	@Override
	public void onInitializeClient() {
		keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.japaneseresolver.resolve", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y, "category.japaneseresolver.resolve"));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (keyBinding.wasPressed()) {
				String jpText = client.player.getMainHandStack().getName().getString();
				client.player.sendMessage(new LiteralText("resolving " + jpText + "..."), false);
				List<String> resolvedJapaneseParts = resolveJapanese(jpText);

				for (String part : resolvedJapaneseParts) {
					
					client.player.sendMessage(new LiteralText(part), false);
				}
			}
		});
	}

	private List<String> resolveJapanese(String jpText) {
		try {
			String jsonResponse = queryJisho(jpText);

			JsonObject topLevel = JsonParser.parseString(jsonResponse).getAsJsonObject();

			JsonArray jishoResponseData = topLevel.get("data").getAsJsonArray();
			
			List<String> parts = new ArrayList<String>();
			for (JsonElement jsonElement : jishoResponseData) {
				String resolvedText = handleJishoEntry(jsonElement.getAsJsonObject());
				parts.add(resolvedText);
			}
			return parts.subList(0, 4); // Only show 5 first results.
		} catch (Exception e) {
			return List.of("Something went wrong");
		}
	}

	private String handleJishoEntry(JsonObject entryObject) {
		JsonObject japanese = entryObject.get("japanese").getAsJsonArray().get(0).getAsJsonObject();

		String resolvedText = japanese.get("word").getAsString();

		String reading = japanese.get("reading").getAsString();

		JsonArray senses = entryObject.get("senses").getAsJsonArray();

		JsonObject firstSense = senses.get(0).getAsJsonObject();

		JsonArray meanings = firstSense.get("english_definitions").getAsJsonArray();

		String firstMeaning = meanings.get(0).getAsString();

		return resolvedText + " => " + "reading: " + reading + " | meaning: " + firstMeaning;
	}

	private String queryJisho(String jpText) throws Exception{
		HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://jisho.org/api/v1/search/words?keyword=".concat(jpText)))
                .build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		return response.body();
	}
	
}
