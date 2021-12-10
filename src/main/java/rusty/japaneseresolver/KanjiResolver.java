package rusty.japaneseresolver;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.gson.*;

public class KanjiResolver {

	private int numEntries = 3;

	public KanjiResolver() {}
	
	public List<String> resolveAllKanjiInString(String text) {
		try {
			String jsonResponse = queryJotoba(text);
			
			JsonObject topLevel = JsonParser.parseString(jsonResponse).getAsJsonObject();

			JsonArray allKanji = topLevel.get("kanji").getAsJsonArray();

			Stream<JsonElement> responseStream = StreamSupport.stream(allKanji.spliterator(), false);

			List<String> resolvedKanji = responseStream.limit(numEntries).map(je -> handleJotobaKanji(je.getAsJsonObject())).toList();

			return resolvedKanji;

		} catch (Exception e) {
			return List.of("Something went wrong");
		}

	}

	private String handleJotobaKanji(JsonObject kanjiObject) {
		String literal = kanjiObject.get("literal").getAsString();
		JsonArray meanings = kanjiObject.getAsJsonArray("meanings");

		List<String> meaningsList = StreamSupport.stream(meanings.spliterator(), false)
			.map(m -> m.getAsString())
			.toList();

		Optional<List<String>> onyomi = Optional.empty();
		Optional<List<String>> kunyomi = Optional.empty();

		if (kanjiObject.has("onyomi")) {
			List<String> onyomiList = StreamSupport.stream(kanjiObject.getAsJsonArray("onyomi").spliterator(), false)
				.map(o -> o.getAsString())
				.toList();
			onyomi = Optional.of(onyomiList);
		}

		if (kanjiObject.has("kunyomi")) {
			List<String> kunyomiList = StreamSupport.stream(kanjiObject.getAsJsonArray("kunyomi").spliterator(), false)
				.map(k -> k.getAsString())
				.toList();
			kunyomi = Optional.of(kunyomiList);
		}

		Kanji kanji = new Kanji(literal, meaningsList, onyomi, kunyomi);

		return kanji.toString();
	}


	private String queryJotoba(String jpText) throws Exception{
		String url = "https://jotoba.de/api/search/kanji";
		HttpClient client = HttpClient.newHttpClient();

		JsonObject requestBody = new JsonObject();
		requestBody.addProperty("query", jpText);
		requestBody.addProperty("language", "English");
		requestBody.addProperty("no_english", false);
		String requestBodyJson = new Gson().toJson(requestBody);

		HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
				.POST(BodyPublishers.ofString(requestBodyJson))
				.header("Content-Type", "application/json")
                .build();

		JapaneseResolver.LOGGER.info("API request: " + url);
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		// JapaneseResolver.LOGGER.info("Response: " + response.body());
		return response.body();
	}
}
