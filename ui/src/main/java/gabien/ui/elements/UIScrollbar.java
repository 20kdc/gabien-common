/*
 * gabien-common - Cross-platform game and UI framework
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * A copy of the Unlicense should have been supplied as COPYING.txt in this repository. Alternatively, you can find it at <https://unlicense.org/>.
 */

package gabien.ui.elements;

import org.eclipse.jdt.annotation.Nullable;

import gabien.render.IGrDriver;
import gabien.ui.IPointerReceiver;
import gabien.ui.UIElement;
import gabien.ui.UILayer;
import gabien.ui.theming.IBorder;
import gabien.ui.theming.IIcon;
import gabien.ui.theming.Theme;
import gabien.uslx.append.Rect;
import gabien.uslx.append.Size;
import gabien.wsi.IPeripherals;
import gabien.wsi.IPointer;

/**
 * Replacement for UIVScrollbar in gabien-app-r48.
 * ... Wow, this didn't need much work to update on feb17.2018 for the Change.
 * Probably because it has *almost no state*.
 * Then again, theming is important, so it did get changed to use borders with a side of more borders.
 * Created on 08/06/17, reworked 22nd July 2022
 */
public class UIScrollbar extends UIElement {
    public double scrollPoint = 0.0;
    public double wheelScale = 0.1;
    public final boolean vertical;

    // size of bar in pixels
    private int barSize;
    private int barLength;
    private int carriageBorder;
    private int carriageMargin;
    private int carriageFloorLength;
    private int nubBorder;
    private int nubSize;
    private double carriageFloorNubPositionScalingFactor;
    private double negativeButtonTimer;
    private double positiveButtonTimer;

    // used during metrics recalc.
    private int sbSize;

    // zones
    private Rect boxNegative = Rect.ZERO;
    private @Nullable Rect boxCarriage;
    private @Nullable Rect boxCarriageFloor;
    private Rect boxPositive = Rect.ZERO;

    public UIScrollbar(boolean vert, int sc) {
        // UIBorderedElement tries to he helpful, but we don't like it
        vertical = vert;
        setSBSize(sc);
        setForcedBounds(null, new Rect(getWantedSize()));
    }

    @Override
    protected void layoutRunImpl() {
        Size size = getSize();

        barSize = vertical ? size.width : size.height;
        barLength = vertical ? size.height : size.width;

        carriageBorder = barSize / 8;
        int carriageLength = barLength - (barSize * 2);
        carriageMargin = carriageBorder;
        if (UIBorderedElement.getMoveDownFlag(getTheme(), Theme.B_SBNUB))
            carriageMargin = 0;

        nubSize = barSize - (carriageMargin * 2);
        int n3 = nubSize / 3;
        // Valid values are 1, 3, 6...
        if (n3 < 3)
            n3 = 1;
        n3 = (n3 / 3) * 3;
        nubBorder = n3;

        if (carriageLength < (barSize * 2)) {
            // not enough room for carriage
            boxCarriage = null;
            boxCarriageFloor = null;
            if (vertical) {
                boxNegative = new Rect(0, 0, barSize, barLength >> 1);
                boxPositive = new Rect(0, boxNegative.height, barSize, barLength - boxNegative.height);
            } else {
                boxNegative = new Rect(0, 0, barLength >> 1, barSize);
                boxPositive = new Rect(boxNegative.width, 0, barLength - boxNegative.width, barSize);
            }
        } else {
            // carriage-layout
            boxNegative = new Rect(0, 0, barSize, barSize);
            if (vertical) {
                boxPositive = new Rect(0, barLength - barSize, barSize, barSize);
                boxCarriage = new Rect(0, barSize, barSize, carriageLength);
            } else {
                boxPositive = new Rect(barLength - barSize, 0, barSize, barSize);
                boxCarriage = new Rect(barSize, 0, carriageLength, barSize);
            }
            boxCarriageFloor = new Rect(boxCarriage.x + carriageMargin, boxCarriage.y + carriageMargin, boxCarriage.width - (carriageMargin * 2), boxCarriage.height - (carriageMargin * 2));
            carriageFloorLength = vertical ? boxCarriageFloor.height : boxCarriageFloor.width;
            carriageFloorNubPositionScalingFactor = carriageFloorLength - nubSize;
        }
    }

    @Override
    public void renderLayer(IGrDriver igd, UILayer layer) {
        if (layer != UILayer.Content)
            return;
        // Negative and Positive Buttons
        Theme theme = getTheme();
        drawNPB(theme, igd, negativeButtonTimer, boxNegative, vertical ? Theme.IC_ARROW_UP : Theme.IC_ARROW_LEFT);
        drawNPB(theme, igd, positiveButtonTimer, boxPositive, vertical ? Theme.IC_ARROW_DOWN : Theme.IC_ARROW_RIGHT);
        if (boxCarriage != null) {
            // Carriage
            UIBorderedElement.drawBorder(theme, igd, Theme.B_SBTRAY, carriageBorder, boxCarriage);
            // Nub
            int nubX = boxCarriageFloor.x;
            int nubY = boxCarriageFloor.y;
            int nubPoint = ((int) Math.ceil(scrollPoint * carriageFloorNubPositionScalingFactor));
            if (vertical) {
                nubY += nubPoint;
            } else {
                nubX += nubPoint;
            }

            UIBorderedElement.drawBorder(theme, igd, Theme.B_SBNUB, nubBorder, nubX, nubY, nubSize, nubSize);
        }
    }

    private void drawNPB(Theme theme, IGrDriver igd, double timer, Rect box, Theme.Attr<IIcon> icoAttr) {
        boolean down = timer > 0;
        Theme.Attr<IBorder> borderId = down ? Theme.B_BTNP : Theme.B_BTN;
        int iconSize = Math.min(box.width, box.height) - (carriageBorder * 2);
        int yOffset = 0;
        if (UIBorderedElement.getMoveDownFlag(theme, borderId))
            yOffset += carriageBorder;
        UIBorderedElement.drawBorder(theme, igd, borderId, carriageBorder, box.x, box.y + yOffset, box.width, box.height);
        IIcon icon = icoAttr.get(getTheme());
        icon.draw(igd, box.x + ((box.width - iconSize) / 2), box.y + yOffset + ((box.height - iconSize) / 2), iconSize);
    }

    @Override
    public void update(double deltaTime, boolean selected, IPeripherals peripherals) {
        if (negativeButtonTimer >= 0)
            negativeButtonTimer -= deltaTime;
        if (positiveButtonTimer >= 0)
            positiveButtonTimer -= deltaTime;
    }

    @Override
    public IPointerReceiver handleNewPointer(IPointer state) {
        if ((boxCarriage != null) && boxCarriage.contains(state.getX(), state.getY())) {
            // This could be improved, but this will do for now.
            return new IPointerReceiver() {
                @Override
                public void handlePointerBegin(IPointer state) {
    
                }
    
                @Override
                public void handlePointerUpdate(IPointer pointer) {
                    if (boxCarriage == null)
                        return;

                    // intended range is 0 - (carriageFloorLength - nubSize)
                    int source;
                    if (vertical) {
                        source = pointer.getY() - boxCarriageFloor.y;
                    } else {
                        source = pointer.getX() - boxCarriageFloor.x;
                    }
                    source -= nubSize / 2;

                    scrollPoint = source / carriageFloorNubPositionScalingFactor;
                    if (scrollPoint < 0)
                        scrollPoint = 0;
                    if (scrollPoint > 1)
                        scrollPoint = 1;
                }
    
                @Override
                public void handlePointerEnd(IPointer state) {
    
                }
            };
        } else if (boxNegative.contains(state.getX(), state.getY())) {
            return new IPointerReceiver() {
                @Override
                public void handlePointerBegin(IPointer state) {
                    handleMousewheel(0, 0, true);
                    negativeButtonTimer = 0.25d;
                }
                @Override
                public void handlePointerUpdate(IPointer state) {
                }
                @Override
                public void handlePointerEnd(IPointer state) {
                }
            };
        } else if (boxPositive.contains(state.getX(), state.getY())) {
            return new IPointerReceiver() {
                @Override
                public void handlePointerBegin(IPointer state) {
                    handleMousewheel(0, 0, false);
                    positiveButtonTimer = 0.25d;
                }
                @Override
                public void handlePointerUpdate(IPointer state) {
                }
                @Override
                public void handlePointerEnd(IPointer state) {
                }
            };
        } else {
            return null;
        }
    }

    @Override
    public void handleMousewheel(int x, int y, boolean north) {
        scrollPoint += north ? -wheelScale : wheelScale;
        if (scrollPoint < 0)
            scrollPoint = 0;
        if (scrollPoint > 1)
            scrollPoint = 1;
    }

    public void setSBSize(int sbSize) {
        this.sbSize = sbSize;
        layoutRecalculateMetrics();
    }

    @Override
    protected @Nullable Size layoutRecalculateMetricsImpl() {
        return new Size(vertical ? sbSize : sbSize * 4, vertical ? sbSize * 4 : sbSize);
    }
}
