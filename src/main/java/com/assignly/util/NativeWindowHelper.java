package com.assignly.util;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.WindowProc;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import javafx.stage.Stage;

public class NativeWindowHelper {

    public interface User32Ex extends StdCallLibrary {
        User32Ex INSTANCE = Native.load("user32", User32Ex.class, W32APIOptions.DEFAULT_OPTIONS);

        HWND FindWindow(String lpClassName, String lpWindowName);
        Pointer SetWindowLongPtr(HWND hWnd, int nIndex, com.sun.jna.Callback wndProc);
        int SetWindowLong(HWND hWnd, int nIndex, com.sun.jna.Callback wndProc);
        LRESULT CallWindowProc(Pointer lpPrevWndFunc, HWND hWnd, int Msg, WPARAM wParam, LPARAM lParam);
        boolean SetWindowPos(HWND hWnd, HWND hWndInsertAfter, int X, int Y, int cx, int cy, int uFlags);
        boolean GetWindowRect(HWND hWnd, WinDef.RECT rect);
    }

    private static WindowProc newWndProc;
    private static Pointer oldWndProcPtr;
    private static HWND hwnd;

    private static final int WM_NCCALCSIZE = 0x0083;
    private static final int WM_NCHITTEST = 0x0084;
    private static final int WM_GETMINMAXINFO = 0x0024;
    private static final int GWLP_WNDPROC = -4;

    private static final int HTCLIENT = 1;
    private static final int HTCAPTION = 2;
    private static final int MONITOR_DEFAULTTONEAREST = 2;

    public static void applyNativeBorderless(Stage stage, String windowTitle, double titleBarHeight) {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) return;

        javafx.application.Platform.runLater(() -> {
            hwnd = User32Ex.INSTANCE.FindWindow(null, windowTitle);
            if (hwnd == null) return;

            newWndProc = new WindowProc() {
                @Override
                public LRESULT callback(HWND hWnd, int uMsg, WPARAM wParam, LPARAM lParam) {
                    if (uMsg == WM_NCCALCSIZE) {
                        if (wParam.intValue() == 1) {
                            return new LRESULT(0); // Hide native title bar completely
                        }
                    } else if (uMsg == WM_GETMINMAXINFO) {
                        LRESULT lRes = User32Ex.INSTANCE.CallWindowProc(oldWndProcPtr, hWnd, uMsg, wParam, lParam);
                        try {
                            Pointer mmi = new Pointer(lParam.longValue());
                            
                            WinUser.HMONITOR hMonitor = User32.INSTANCE.MonitorFromWindow(hWnd, MONITOR_DEFAULTTONEAREST);
                            if (hMonitor != null) {
                                WinUser.MONITORINFO monitorInfo = new WinUser.MONITORINFO();
                                // Ensure cbSize is set
                                monitorInfo.cbSize = monitorInfo.size();
                                WinDef.BOOL success = User32.INSTANCE.GetMonitorInfo(hMonitor, monitorInfo);
                                
                                if (success != null && success.booleanValue()) {
                                    WinDef.RECT rcWork = monitorInfo.rcWork;
                                    WinDef.RECT rcMonitor = monitorInfo.rcMonitor;
                                    
                                    int maxPosX = Math.abs(rcWork.left - rcMonitor.left);
                                    int maxPosY = Math.abs(rcWork.top - rcMonitor.top);
                                    int maxSizeX = Math.abs(rcWork.right - rcWork.left);
                                    int maxSizeY = Math.abs(rcWork.bottom - rcWork.top);
                                    
                                    // Fix for Auto-Hide Taskbar:
                                    // If work area equals monitor area, Windows treats the borderless window 
                                    // as a fullscreen exclusive app (like a game) and blocks the auto-hide taskbar.
                                    // Subtracting 1 pixel breaks this heuristic and allows the taskbar to slide up.
                                    if (maxSizeX == Math.abs(rcMonitor.right - rcMonitor.left) &&
                                        maxSizeY == Math.abs(rcMonitor.bottom - rcMonitor.top)) {
                                        maxSizeY -= 1;
                                    }
                                    
                                    // Memory Layout of MINMAXINFO:
                                    // 0: ptReserved.x, 4: ptReserved.y
                                    // 8: ptMaxSize.x, 12: ptMaxSize.y
                                    // 16: ptMaxPosition.x, 20: ptMaxPosition.y
                                    mmi.setInt(16, maxPosX);
                                    mmi.setInt(20, maxPosY);
                                    mmi.setInt(8, maxSizeX);
                                    mmi.setInt(12, maxSizeY);
                                }
                            }
                        } catch (Throwable t) {
                            System.err.println("Exception in WM_GETMINMAXINFO: " + t);
                        }
                        return lRes;
                    } else if (uMsg == WM_NCHITTEST) {
                        LRESULT lRes = User32Ex.INSTANCE.CallWindowProc(oldWndProcPtr, hWnd, uMsg, wParam, lParam);
                        int hit = lRes.intValue();
                        if (hit == HTCLIENT) {
                            int x = (short)(lParam.longValue() & 0xFFFF);
                            int y = (short)((lParam.longValue() >> 16) & 0xFFFF);

                            WinDef.RECT rect = new WinDef.RECT();
                            User32Ex.INSTANCE.GetWindowRect(hWnd, rect);
                            
                            // Check if mouse is in the title bar area
                            // Using screen scale to correctly calculate hit area on high DPI displays
                            double scale = javafx.stage.Screen.getPrimary().getOutputScaleY();
                            if (y >= rect.top && y < rect.top + (int)(titleBarHeight * scale)) {
                                // Keep the right side reserved for custom buttons (minimize, maximize, close)
                                int rightEdge = rect.right - rect.left;
                                int mouseXLocal = x - rect.left;
                                if (mouseXLocal < rightEdge - (160 * scale)) {
                                    return new LRESULT(HTCAPTION);
                                }
                            }
                        }
                        return lRes;
                    }
                    return User32Ex.INSTANCE.CallWindowProc(oldWndProcPtr, hWnd, uMsg, wParam, lParam);
                }
            };

            if (Native.POINTER_SIZE == 8) {
                oldWndProcPtr = User32Ex.INSTANCE.SetWindowLongPtr(hwnd, GWLP_WNDPROC, newWndProc);
            } else {
                int oldWndProc = User32Ex.INSTANCE.SetWindowLong(hwnd, GWLP_WNDPROC, newWndProc);
                oldWndProcPtr = new Pointer(oldWndProc);
            }

            if (oldWndProcPtr == null || Pointer.nativeValue(oldWndProcPtr) == 0) {
                System.err.println("Failed to subclass window procedure for native borderless.");
                return;
            }

            // Force window to redraw and recalculate frame size
            User32Ex.INSTANCE.SetWindowPos(hwnd, null, 0, 0, 0, 0, 
                WinUser.SWP_NOMOVE | WinUser.SWP_NOSIZE | WinUser.SWP_NOZORDER | WinUser.SWP_FRAMECHANGED);
        });
    }
}
