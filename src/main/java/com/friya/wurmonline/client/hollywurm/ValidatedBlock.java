package com.friya.wurmonline.client.hollywurm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ValidatedBlock
{
	private List<String[]> blockData = new ArrayList<String[]>();
	private long startAtMs = 0;
	
	ValidatedBlock(String[] args, long startAt)
	{
		if(args == null) {
			return;
		}
		
		startAtMs = startAt;
		addArg(args);
	}
	
	ValidatedBlock(String[] args)
	{
		if(args == null) {
			return;
		}
		
		addArg(args);
	}

	public ValidatedBlock() 
	{
	}
	
	long getStartTime()
	{
		return startAtMs;
	}
	
	boolean addArg(String[] args)
	{
		blockData.add(args);
		return true;
	}
	
	List<String[]> getData()
	{
		return blockData;
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < blockData.size(); i++) {
			sb.append("\t" + (i > 0 ? "\t" : "") + Arrays.toString(addAssumedQuotes(blockData.get(i))) + "\n");
		}
		
		return sb.toString();
	}
	
	/*
	 * This is a hack that will not be used for anything except debugging.
	 * Add quotes around strings that are likely to need quotes (i.e. they have a space in them).
	 */
	private String[] addAssumedQuotes(String[] args)
	{
		for(int i = 0; i < args.length; i++) {
			if(args[i].contains(" ")) {
				args[i] = "\"" + args[i] + "\"";
			}
		}
		
		return args;
	}
}
