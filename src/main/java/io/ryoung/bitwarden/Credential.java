package io.ryoung.bitwarden;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import lombok.Getter;

final class Credential
{
	@Getter
	private final String username;

	@Getter
	private final char[] password;

	static final Type TYPE = new TypeToken<List<Credential>>()
	{
	}.getType();

	private Credential(JsonObject jsonObject)
	{
		this.username = jsonObject.get("username").getAsString();
		this.password = jsonObject.get("password").getAsString().toCharArray();
	}

	static final class Deserializer implements JsonDeserializer<Credential>
	{
		@Override
		public Credential deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException
		{
			JsonObject jsonObject = json.getAsJsonObject();

			return Optional.ofNullable(jsonObject.get("login"))
				.map(JsonElement::getAsJsonObject)
				.map(Credential::new)
				.orElse(null);
		}
	}
}


