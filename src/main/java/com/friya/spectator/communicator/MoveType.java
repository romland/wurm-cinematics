package com.friya.spectator.communicator;

import com.google.gson.annotations.SerializedName;

// https://stackoverflow.com/questions/8211304/using-enums-while-parsing-json-with-gson
public enum MoveType 
{
	@SerializedName("0") HOVER,
	@SerializedName("1") FOLLOW,
	@SerializedName("2") PAN_AROUND,
	@SerializedName("3") CIRCLE,
	
	@SerializedName("254") NONE,			// just stand still for N seconds
	@SerializedName("255") ANY				// pick any of the moves on the client
}
