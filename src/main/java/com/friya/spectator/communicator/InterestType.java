package com.friya.spectator.communicator;

import com.google.gson.annotations.SerializedName;

// https://stackoverflow.com/questions/8211304/using-enums-while-parsing-json-with-gson
public enum InterestType 
{
	@SerializedName("0") AREA,
	@SerializedName("1") STATIC_OBJECT,
	@SerializedName("2") TILE,
	@SerializedName("3") ANNOUNCEMENT,
	@SerializedName("4") MOVING_OBJECT
}
