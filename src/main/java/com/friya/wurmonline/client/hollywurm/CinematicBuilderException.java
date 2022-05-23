package com.friya.wurmonline.client.hollywurm;

public class CinematicBuilderException extends RuntimeException
{
	private static final long serialVersionUID = 4012446400591080470L;

	CinematicBuilderException(String msg)
	{
		super(msg);
	}

	CinematicBuilderException(int line, String msg)
	{
		super("You have a mistake on line " + line + ": " + msg);
	}

	CinematicBuilderException(int line, String msg, String fileName)
	{
		super("You have a mistake in script \"" + fileName + "\" on line " + line + ": " + msg);
	}
}
