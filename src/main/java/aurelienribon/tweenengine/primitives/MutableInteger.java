package aurelienribon.tweenengine.primitives;

import aurelienribon.tweenengine.Tween;
import aurelienribon.tweenengine.TweenAccessor;

/**
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
@SuppressWarnings("serial")
public class MutableInteger extends Number implements TweenAccessor<MutableInteger> {
	private int value;

	public MutableInteger(int value) {
		this.value = value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	@Override public int intValue() {return (int) value;}
	@Override public long longValue() {return (long) value;}
	@Override public float floatValue() {return (float) value;}
	@Override public double doubleValue() {return (double) value;}

	@Override
	public int getValues(Tween tween, MutableInteger target, int tweenType, float[] returnValues) {
		returnValues[0] = target.value;
		return 1;
	}

	@Override
	public void setValues(Tween tween, MutableInteger target, int tweenType, float[] newValues) {
		target.value = (int) newValues[0];
	}
}
