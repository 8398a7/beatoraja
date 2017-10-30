package bms.player.beatoraja.input;


import java.util.Arrays;
import java.util.logging.Logger;

import bms.player.beatoraja.Config;
import bms.player.beatoraja.PlayConfig.ControllerConfig;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;

/**
 * 専用コントローラー入力処理用クラス
 * 
 * @author exch
 */
public class BMControllerInputProcessor extends BMSPlayerInputDevice implements ControllerListener {

	// TODO アナログ皿対応

	private final BMSPlayerInputProcessor bmsPlayerInputProcessor;

	private Controller controller;

	/**
	 * ボタンキーアサイン
	 */
	private int[] buttons = new int[] { BMKeys.BUTTON_4, BMKeys.BUTTON_7, BMKeys.BUTTON_3, BMKeys.BUTTON_8,
			BMKeys.BUTTON_2, BMKeys.BUTTON_5, BMKeys.LEFT, BMKeys.UP, BMKeys.DOWN };
	/**
	 * スタートキーアサイン
	 */
	private int start = BMKeys.BUTTON_9;
	/**
	 * セレクトキーアサイン
	 */
	private int select = BMKeys.BUTTON_10;

	private float[] axis = new float[4];
	private boolean[] analogaxis = new boolean[4];
	private long[] axistime = new long[4];

	private int lastPressedButton = -1;
	
        private boolean koc = false;
	
        private Config config;
	
	public BMControllerInputProcessor(BMSPlayerInputProcessor bmsPlayerInputProcessor, Controller controller,
	                                  ControllerConfig controllerConfig) {
		super(Type.BM_CONTROLLER);
		config = new Config();
		koc = config.getJKOC();
		this.bmsPlayerInputProcessor = bmsPlayerInputProcessor;
		this.controller = controller;
		this.setConfig(controllerConfig);
	}

	public void setConfig(ControllerConfig controllerConfig) {
		this.buttons = controllerConfig.getKeyAssign().clone();
		this.start = controllerConfig.getStart();
		this.select = controllerConfig.getSelect();
	}
	
	public Controller getController() {
		return controller;
	}

	public boolean accelerometerMoved(Controller arg0, int arg1, Vector3 arg2) {
		Logger.getGlobal().info("controller : " + controller.getName() + " accelerometer moved :" + arg1 + " - "
				+ arg2.x + " " + arg2.y + " " + arg2.z);
		return false;
	}

	public boolean axisMoved(Controller arg0, int arg1, float arg2) {
		return false;
	}

	public boolean buttonDown(Controller arg0, int keycode) {
		return false;
	}

	public boolean buttonUp(Controller arg0, int keycode) {
		return false;
	}

	public void connected(Controller arg0) {
	}

	public void disconnected(Controller arg0) {
	}

	public boolean povMoved(Controller arg0, int arg1, PovDirection arg2) {
		Logger.getGlobal()
				.info("controller : " + controller.getName() + "pov moved : " + arg1 + " - " + arg2.ordinal());
		return false;
	}

	public boolean xSliderMoved(Controller arg0, int arg1, boolean arg2) {
		Logger.getGlobal().info("controller : " + controller.getName() + "xslider moved : " + arg1 + " - " + arg2);
		return false;
	}

	public boolean ySliderMoved(Controller arg0, int arg1, boolean arg2) {
		Logger.getGlobal().info("controller : " + controller.getName() + "yslider moved : " + arg1 + " - " + arg2);
		return false;
	}

	private final boolean[] buttonstate = new boolean[20];
	private final boolean[] buttonchanged = new boolean[20];
	private final long[] buttontime = new long[20];

	private int duration = 16;

	public void clear() {
		// Arrays.fill(buttonstate, false);
		// Arrays.fill(axis, 0);
		Arrays.fill(buttonchanged, false);
		Arrays.fill(buttontime, Long.MIN_VALUE);
		lastPressedButton = -1;
	}

	/**
	 * スクラッチ停止カウンタ
	 */
	private long counter = 1;
	/**
	 * アナログスクラッチ位置(-1<->0<->1)
	 */
	private float oldAnalogScratchX = 10;
	/**
	 * アナログスクラッチ 入力フラグ
	 */
	private boolean activeAnalogScratch = false;
	/**
	 * アナログスクラッチ 右回転フラグ
	 */
	private boolean rightMoveScratching = false;

	public void poll(final long presstime) {
		for (int i = 0; i < 4; i++) {
			final float ax = controller.getAxis(i);
			if (analogaxis[i] && !koc) {
				if ((axis[i] == 1.0 && ax == -1.0) || (axis[i] < 1.0 && ax > axis[i])) {
					this.bmsPlayerInputProcessor.keyChanged(this, (int) presstime, 8 + player * 9, false);
					this.bmsPlayerInputProcessor.keyChanged(this, (int) presstime, 7 + player * 9, true);
					this.axistime[i] = presstime;
				} else if ((axis[i] == -1.0 && ax == 1.0) || (axis[i] > -1.0 && ax > axis[i])) {
					this.bmsPlayerInputProcessor.keyChanged(this, (int) presstime, 8 + player * 9, true);
					this.bmsPlayerInputProcessor.keyChanged(this, (int) presstime, 7 + player * 9, false);
					this.axistime[i] = presstime;
				} else if (axistime[i] != -1 && presstime > axistime[i] + 50) {
					this.bmsPlayerInputProcessor.keyChanged(this, (int) presstime, 8 + player * 9, false);
					this.bmsPlayerInputProcessor.keyChanged(this, (int) presstime, 7 + player * 9, false);
					this.axistime[i] = -1;
				}
			} else {
				if ((ax > -0.9 && ax < -0.1) || (ax > 0.1 && ax < 0.9)) {
					if (axistime[i] != -1) {
						if (presstime > axistime[i] + 500  && !koc) {
							if(!koc)
								analogaxis[i] = false;
							else
								analogaxis[i] = true;
						}
					} else {
						axistime[i] = presstime;
					}
				} else {
					axistime[i] = -1;
				}
			}
			axis[i] = ax;
		}

		for (int button = 0; button < buttonstate.length; button++) {
			if (presstime >= buttontime[button] + duration) {
				final boolean prev = buttonstate[button];
				if (button <= BMKeys.BUTTON_16) {
					buttonstate[button] = controller.getButton(button);
				} else if (button == BMKeys.UP && !koc) {
					buttonstate[button] = scratchInput(BMKeys.UP);
					buttonstate[button] = activeAnalogScratch && rightMoveScratching;
				} else if (button == BMKeys.DOWN && !koc) {
					buttonstate[button] = scratchInput(BMKeys.DOWN);
					buttonstate[button] = activeAnalogScratch && !rightMoveScratching;
				} else if (button == BMKeys.LEFT) {
					buttonstate[button] = (!analogaxis[0] && axis[0] < -0.9) || (!analogaxis[3] && axis[3] < -0.9);
				} else if (button == BMKeys.RIGHT) {
					buttonstate[button] = (!analogaxis[0] && axis[0] > 0.9) || (!analogaxis[3] && axis[3] > 0.9);
				}
				if (buttonchanged[button] = (prev != buttonstate[button])) {
					buttontime[button] = presstime;
				}

				if (!prev && buttonstate[button]) {
					setLastPressedButton(button);
				}
			}
		}

		for (int i = 0; i < buttons.length; i++) {
			if (buttonchanged[buttons[i]]) {
				this.bmsPlayerInputProcessor.keyChanged(this, presstime, i + player * 9, buttonstate[buttons[i]]);
				buttonchanged[buttons[i]] = false;
			}
		}

		if (buttonchanged[start]) {
			this.bmsPlayerInputProcessor.startChanged(buttonstate[start]);
			buttonchanged[start] = false;
		}
		if (buttonchanged[select]) {
			this.bmsPlayerInputProcessor.setSelectPressed(buttonstate[select]);
			buttonchanged[select] = false;
		}
	}

	private boolean scratchInput(int button) {
		if(!config.isAnalogScratch()) {
			if(button == BMKeys.UP) {
				return (!analogaxis[1] && axis[1] < -0.9) || (!analogaxis[2] && axis[2] < -0.9);
			} else if(button == BMKeys.DOWN){
				return (!analogaxis[1] && axis[1] > 0.9) || (!analogaxis[2] && axis[2] > 0.9);
			}
		}
		
		float analogScratchX = axis[1];
		if (oldAnalogScratchX > 1) {
			oldAnalogScratchX = analogScratchX;
			activeAnalogScratch = false;
			return false;
		}

		if (oldAnalogScratchX != analogScratchX) {
			boolean nowRight = false;
			if (oldAnalogScratchX < analogScratchX) {
				nowRight = true;
				if ((analogScratchX - oldAnalogScratchX) > (1 - analogScratchX + oldAnalogScratchX)) {
					nowRight = false;
				}
			} else if (oldAnalogScratchX > analogScratchX) {
				nowRight = false;
				if ((oldAnalogScratchX - analogScratchX) > ((analogScratchX + 1) - oldAnalogScratchX)) {
					nowRight = true;
				}
			}

			if (activeAnalogScratch && !(rightMoveScratching == nowRight)) {
				rightMoveScratching = nowRight;

			} else if (!activeAnalogScratch) {
				activeAnalogScratch = true;
				rightMoveScratching = nowRight;
			}

			counter = 0;
			oldAnalogScratchX = analogScratchX;
		}

		// counter > 100 ... Stop Scratching.
		if (counter > 100 && activeAnalogScratch) {
			activeAnalogScratch = false;
			counter = 0;
		}

		if (counter == Long.MAX_VALUE) {
			counter = 0;
		}

		counter++;
		
		if(button == BMKeys.UP) {
			return activeAnalogScratch && rightMoveScratching;
		} else if(button == BMKeys.DOWN){
			return activeAnalogScratch && !rightMoveScratching;
		} else {
		    return false;
		}
	}

	public int getLastPressedButton() {
		return lastPressedButton;
	}

	public void setLastPressedButton(int lastPressedButton) {
		this.lastPressedButton = lastPressedButton;
	}

	public void setMinimumDuration(int duration) {
		this.duration = duration;
	}

	public static class BMKeys {

		public static final int BUTTON_1 = 0;
		public static final int BUTTON_2 = 1;
		public static final int BUTTON_3 = 2;
		public static final int BUTTON_4 = 3;
		public static final int BUTTON_5 = 4;
		public static final int BUTTON_6 = 5;
		public static final int BUTTON_7 = 6;
		public static final int BUTTON_8 = 7;
		public static final int BUTTON_9 = 8;
		public static final int BUTTON_10 = 9;
		public static final int BUTTON_11 = 10;
		public static final int BUTTON_12 = 11;
		public static final int BUTTON_13 = 12;
		public static final int BUTTON_14 = 13;
		public static final int BUTTON_15 = 14;
		public static final int BUTTON_16 = 15;
		public static final int UP = 16;
		public static final int DOWN = 17;
		public static final int LEFT = 18;
		public static final int RIGHT = 19;

		/**
		 * 専コンのキーコードに対応したテキスト
		 */
		private static final String[] BMCODE = { "BUTTON 1", "BUTTON 2", "BUTTON 3", "BUTTON 4", "BUTTON 5", "BUTTON 6",
				"BUTTON 7", "BUTTON 8", "BUTTON 9", "BUTTON 10", "BUTTON 11", "BUTTON 12", "BUTTON 13", "BUTTON 14",
				"BUTTON 15", "BUTTON 16", "UP", "DOWN", "LEFT", "RIGHT" };

		public static final String toString(int keycode) {
			if (keycode >= 0 && keycode < BMCODE.length) {
				return BMCODE[keycode];
			}
			return "Unknown";
		}
	}

}