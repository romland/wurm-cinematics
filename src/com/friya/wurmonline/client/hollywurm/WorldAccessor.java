package com.friya.wurmonline.client.hollywurm;

import com.wurmonline.client.renderer.terrain.weather.Weather;

import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenAccessor;

/*
Weather.getInstance().setWindDir(windDirSlider.getValue());
Weather.getInstance().setWindForce(windForceSlider.getValue() / 100.0f);
	/world wind for 5s
		target northwest 100		# direction and force

Weather.getInstance().setCloudiness(cloudSlider.getValue() / 100.0f);
	/world clouds for 5s
		target 100

Weather.getInstance().setRain(rainSlider.getValue() / 100.0f);
	/world rain for 5s
		target 100

Weather.getInstance().foginess = fogSlider.getValue() / 100.0f;
	/world fog for 5s
		target 100

WeatherControls.this.world.setWurmTimeOffsetFromTime(hour, minute, 0);
	/world time for 5s
		target 12 30

	public static final int WEATHER_WIND = 15;
	public static final int WEATHER_CLOUDS = 16;
	public static final int WEATHER_RAIN = 17;
	public static final int WEATHER_FOG = 18;
	public static final int WORLD_TIME = 19;
*/
public class WorldAccessor  implements TweenAccessor<Weather>
{

	
	@Override
	public int getValues(Tween tween, Weather target, int tweenType, float[] returnValues)
	{
		switch (tweenType) {
			case TransformTypes.WEATHER_WIND :
				returnValues[0] = getWindDirection(target);
				returnValues[1] = getWindForce(target);
				return 2;
	
			case TransformTypes.WEATHER_CLOUDS :
				returnValues[0] = getClouds(target);
				return 1;
	
			case TransformTypes.WEATHER_RAIN :
				returnValues[0] = getRain(target);
				return 1;
	
			case TransformTypes.WEATHER_FOG :
				returnValues[0] = getFog(target);
				return 1;
	
			case TransformTypes.WORLD_TIME :
				returnValues[0] = getTime(target);
				return 1;
			
			default:
				assert false;
				return 0;
		}
	}

	@Override
	public void setValues(Tween tween, Weather target, int tweenType, float[] newValues)
	{
		switch (tweenType ) {
			case TransformTypes.WEATHER_WIND :
				setWind(target, newValues[0], newValues[1]);
				break;

			case TransformTypes.WEATHER_CLOUDS :
				setClouds(target, newValues[0]);
				break;

			case TransformTypes.WEATHER_RAIN :
				setRain(target, newValues[0]);
				break;

			case TransformTypes.WEATHER_FOG :
				setFog(target, newValues[0]);
				break;

			case TransformTypes.WORLD_TIME :
				setTime(target, newValues[0]);
				break;

			default:
				assert false;
				break;
		}
	}

	
	private float getWindDirection(Weather w)
	{
		return w.windDir;
	}
	
	private float getWindForce(Weather w)
	{
		return w.windForce * 100f;
	}
	
	private float getClouds(Weather w)
	{
		return w.getCloudiness() * 100f;
	}
	
	private float getRain(Weather w)
	{
		return w.rain * 100f;
	}
	
	private float getFog(Weather w)
	{
		return w.foginess * 100f;
	}
	
	private float getTime(Weather w)
	{
		// Seconds
		return WurmHelpers.getWorld().getWurmTime();
	}

	
	private void setWind(Weather w, float dir, float force)
	{
		w.setWeather(w.getCloudiness(), w.foginess, w.rain, dir, force / 100f, w.getTemperature()); 
	}
	
	private void setClouds(Weather w, float clouds)
	{
		w.setWeather(clouds / 100f, w.foginess, w.rain, w.windDir, w.windForce, w.getTemperature()); 
	}
	
	private void setRain(Weather w, float rain)
	{
		w.setWeather(w.getCloudiness(), w.foginess, rain / 100f, w.windDir, w.windForce, w.getTemperature()); 
	}
	
	private void setFog(Weather w, float fog)
	{
		w.setWeather(w.getCloudiness(), fog / 100f, w.rain, w.windDir, w.windForce, w.getTemperature()); 
	}

	private void setTime(Weather w, float time)
	{
		// Seconds
		WurmHelpers.getWorld().setWurmTime((long)time);
	}
}
