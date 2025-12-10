package com.example.game240;

public class Calculate24
{
    public static String resultExpr = null; // 存储第一个找到的表达式

    public static boolean js(double[] num, String[] expr, int n)
    {
        if (n == 1)
        {
            if (Math.abs(num[0] - 24) < 1e-6)
            {
                resultExpr = expr[0];
                return true;
            }
            return false;
        }

        for (int i = 0; i < n - 1; i++)
        {
            for (int j = i + 1; j < n; j++)
            {
                double p = num[i], q = num[j];
                String ep = expr[i], eq = expr[j];

                num[j] = num[n - 1];
                expr[j] = expr[n - 1];

                expr[i] = "(" + ep + "+" + eq + ")";
                num[i] = p + q;
                if (js(num, expr, n - 1)) return true;

                expr[i] = "(" + ep + "*" + eq + ")";
                num[i] = p * q;
                if (js(num, expr, n - 1)) return true;

                expr[i] = "(" + ep + "-" + eq + ")";
                num[i] = p - q;
                if (js(num, expr, n - 1)) return true;

                expr[i] = "(" + eq + "-" + ep + ")";
                num[i] = q - p;
                if (js(num, expr, n - 1)) return true;

                if (Math.abs(q) > 0)
                {
                    expr[i] = "(" + ep + "/" + eq + ")";
                    num[i] = p / q;
                    if (js(num, expr, n - 1)) return true;
                }

                if (Math.abs(p) > 0)
                {
                    expr[i] = "(" + eq + "/" + ep + ")";
                    num[i] = q / p;
                    if (js(num, expr, n - 1)) return true;
                }

                num[i] = p;
                num[j] = q;
                expr[i] = ep;
                expr[j] = eq;
            }
        }

        return false;
    }
}
