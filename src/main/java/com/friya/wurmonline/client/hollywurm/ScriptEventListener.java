package com.friya.wurmonline.client.hollywurm;

interface ScriptEventListener 
{
	void scriptEndedEvent(EventInformationScene scene);
	void scriptStartedEvent(EventInformationScene scene);

	void sceneEndedEvent(EventInformationScene scene);
	void sceneStartedEvent(EventInformationScene scene);
}
