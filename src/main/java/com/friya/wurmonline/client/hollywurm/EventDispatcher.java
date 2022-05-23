package com.friya.wurmonline.client.hollywurm;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
//import java.util.logging.Logger;

public class EventDispatcher
{
    private static final List<EventOnce> events = new CopyOnWriteArrayList<EventOnce>();
//    private static Logger logger = Logger.getLogger(EventDispatcher.class.getName());

    public EventDispatcher()
    {
	}

	static public void add(EventOnce event)
	{
		event.setId(System.currentTimeMillis() + events.size());
		events.add(event);
	}
	
	static public boolean hasPending()
	{
		return events.size() > 0;
	}

	static public void poll()
	{
		if(events.size() == 0) {
			return;
		}

		List<EventOnce> found = new CopyOnWriteArrayList<EventOnce>();
		long ts = System.currentTimeMillis();

		for(EventOnce event : events) {
			if(event.getInvokeAt() < ts) {
				if(event.invoke()) {
					found.add(event);
				}
			}
		}

		if(found.size() > 0) {
			events.removeAll(found);
		}
	}
	
	static public boolean cancel(EventOnce e)
	{
		for(EventOnce ev : events) {
			if(ev.getId() == e.getId()) {
				return events.remove(ev);
			}
		}
		
		return false;
	}

	static public boolean cancelAll()
	{
		events.clear();
		return true;
	}
}
