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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.gson.*;

public class JapaneseResolverClient implements ClientModInitializer {

	private static KeyBinding keyBinding;

	@Override
	public void onInitializeClient() {
		keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.japaneseresolver.resolve", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Y, "category.japaneseresolver.resolve"));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (keyBinding.wasPressed()) {
				String jpText = client.player.getMainHandStack().getName().getString();
				client.player.sendMessage(new LiteralText(" "), false);
				client.player.sendMessage(new LiteralText("Resolving " + jpText + "..."), false);
				new Thread(() -> {
					List<String> resolvedJapaneseParts = resolveJapanese(jpText);
					for (String part : resolvedJapaneseParts) {
						client.player.sendMessage(new LiteralText(part), false);
					}
				}).start();
			}
		});
	}

	private List<String> resolveJapanese(String jpText) {
		try {
			String jsonResponse = queryJisho(jpText);

			JsonObject topLevel = JsonParser.parseString(jsonResponse).getAsJsonObject();

			JsonArray jishoResponseData = topLevel.get("data").getAsJsonArray();
			
			Stream<JsonElement> responseStream = StreamSupport.stream(jishoResponseData.spliterator(), false);
			// Only handles 3 first results.
			List<String> resolvedEntries = responseStream.limit(3).map(je -> handleJishoEntry(je.getAsJsonObject())).toList();
			return resolvedEntries;
		} catch (Exception e) {
			return List.of("Something went wrong");
		}
	}

	private String handleJishoEntry(JsonObject entryObject) {

		try {
			JsonObject japanese = entryObject.get("japanese").getAsJsonArray().get(0).getAsJsonObject();

			Optional<String> word = Optional.empty();
			if (japanese.has("word")) {
				word = Optional.of(japanese.get("word").getAsString());
			}

			Optional<String> reading = Optional.empty();
			if (japanese.has("reading")) {
				reading = Optional.of(japanese.get("reading").getAsString());
			}

			JishoEntry.JapaneseWord jWord = new JishoEntry.JapaneseWord(word, reading);

			JsonArray senses = entryObject.get("senses").getAsJsonArray();

			List<JishoEntry.Sense> sensesList = StreamSupport.stream(senses.spliterator(), false)
				.limit(2) // first 2 senses
				.map((JsonElement s) -> s.getAsJsonObject().get("english_definitions").getAsJsonArray())
				.map(meanings -> 
					StreamSupport.stream(meanings.spliterator(), false)
					.limit(3) // first 3 meanings
					.map(m -> m.getAsString())
					.toList())
				.map((List<String> ms) -> new JishoEntry.Sense(ms))
				.toList();

			JishoEntry entry = new JishoEntry(List.of(jWord), sensesList);

			return entry.toString();
		} catch (Exception e) {
			return "Couldn't resolve entry";
		}
	}

	private String queryJisho(String jpText) throws Exception{
		String url = "https://jisho.org/api/v1/search/words?keyword=".concat(jpText);
		HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
		JapaneseResolver.LOGGER.info("API request: " + url);
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		// JapaneseResolver.LOGGER.info("Response: " + response.body());
		return response.body();
	}
	
}
