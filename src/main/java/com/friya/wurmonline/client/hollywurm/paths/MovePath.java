package com.friya.wurmonline.client.hollywurm.paths;

import java.util.ArrayList;

import com.friya.spectator.communicator.MoveType;
import com.friya.wurmonline.client.hollywurm.TweenPosition;

public interface MovePath {

	MoveType getType();

	ArrayList<TweenPosition> getPath(PathArgs args) throws PathArgException;

}