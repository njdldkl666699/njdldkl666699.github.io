package io.njdldkl;

import java.util.Arrays;
import java.util.Objects;

/**
 * 状态类，表示N数码问题的一个状态
 *
 * @param board        棋盘状态数组，0表示空格
 * @param zeroPosition 空格位置
 */
public record State(byte[][] board, Point2D zeroPosition){

    public State(byte[][] board) {
        this(board, getNumberPosition(board, (byte) 0));
    }

    /**
     * 位置记录
     *
     * @param row 行索引
     * @param col 列索引
     */
    public record Point2D(int row, int col) {
    }

    /**
     * 四个可能的移动方向：上、下、左、右
     */
    private static final Point2D[] DIRECTIONS = {
            new Point2D(-1, 0), // 上
            new Point2D(1, 0), // 下
            new Point2D(0, -1), // 左
            new Point2D(0, 1)  // 右
    };

    /**
     * 获取指定数字在棋盘上的位置
     *
     * @param number 要查找的数字
     * @return 数字的位置，若未找到则返回null
     */
    private static Point2D getNumberPosition(byte[][] board, byte number) {
        int n = board.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j] == number) {
                    return new Point2D(i, j);
                }
            }
        }
        return null;
    }

    public Point2D getNumberPosition(byte number) {
        if (number == 0) {
            return zeroPosition;
        }
        return getNumberPosition(board, number);
    }

    /**
     * 获取当前状态的所有后继状态
     *
     * @return 后继状态数组
     */
    public State[] getSuccessors() {
        int n = board.length;
        State[] successors = new State[4];
        int count = 0;
        int row = zeroPosition.row;
        int col = zeroPosition.col;

        for (var dir : DIRECTIONS) {
            int newRow = row + dir.row;
            int newCol = col + dir.col;

            // 检查新位置是否在棋盘范围内
            if (newRow >= 0 && newRow < n && newCol >= 0 && newCol < n) {
                // 创建新状态的棋盘副本
                byte[][] newBoard = new byte[n][n];
                for (int i = 0; i < n; i++) {
                    newBoard[i] = Arrays.copyOf(board[i], n);
                }

                // 交换空格与相邻数字的位置
                newBoard[row][col] = newBoard[newRow][newCol];
                newBoard[newRow][newCol] = 0;

                // 创建新的状态并添加到后继状态数组中
                successors[count++] = new State(newBoard, new Point2D(newRow, newCol));
            }
        }

        return Arrays.copyOf(successors, count);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        for (byte[] row : board) {
            sb.append(Arrays.toString(row)).append("\n");
        }
        return Objects.toIdentityString(this) + ": \n" + sb;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return Objects.deepEquals(board, state.board);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(board);
    }
}
