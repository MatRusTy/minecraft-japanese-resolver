package rusty.japaneseresolver;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.gson.*;

public class JishoSearch {

	private int numEntries = 3; // default: first 3 results.
	private int numSenses = 2; // default: first 2 senses
	private int numMeanings = 3; // default: first 3 meanings

	public JishoSearch() {}

	public JishoSearch(int entries, int senses, int meanings){
		this.numEntries = entries;
		this.numSenses = senses;
		this.numMeanings = meanings;
	}
	
	public List<String> jishoSearch(String jpText) {
		try {
			String jsonResponse = queryJisho(jpText);

			JsonObject topLevel = JsonParser.parseString(jsonResponse).getAsJsonObject();

			JsonArray jishoResponseData = topLevel.get("data").getAsJsonArray();
			
			Stream<JsonElement> responseStream = StreamSupport.stream(jishoResponseData.spliterator(), false);
			List<String> resolvedEntries = responseStream.limit(numEntries).map(je -> handleJishoEntry(je.getAsJsonObject())).toList();
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
				.limit(numSenses)
				.map((JsonElement s) -> s.getAsJsonObject().get("english_definitions").getAsJsonArray())
				.map(meanings -> 
					StreamSupport.stream(meanings.spliterator(), false)
					.limit(numMeanings)
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
