package com.tituuuu.models;

public class UniversalStructElement4 {
    private final double[] ksi;
    private final double[] eta;
    private final double[][] dNdKsi;
    private final double[][] dNdEta;
    private final double[][] N;
    private final double[] detJacobians;
    private final double[][] invertedJacobians;
    private final double[] w;
    private final Side[] sides;

    private class Side {
        public double[] ksi;
        public double[] eta;
        public double[][] N;
        public double detJ;

        public Side() {
            ksi = new double[2];
            eta = new double[2];
            N = new double[2][4];
        }
    }

    public UniversalStructElement4(double[] x, double[] y, int npc) {
        switch (npc) {
            case 2:
                w = new double[]{1, 1};
                ksi = new double[]{-1 / Math.sqrt(3), 1 / Math.sqrt(3), 1 / Math.sqrt(3), -1 / Math.sqrt(3)};
                eta = new double[]{-1 / Math.sqrt(3), -1 / Math.sqrt(3), 1 / Math.sqrt(3), 1 / Math.sqrt(3)};

                sides = new Side[]{new Side(), new Side(), new Side(), new Side()};
                double[] tempksi = new double[]{
                        -1 / Math.sqrt(3),
                        1 / Math.sqrt(3),
                        1,
                        1,
                        1 / Math.sqrt(3),
                        -1 / Math.sqrt(3),
                        -1,
                        -1};
                double[] tempeta = new double[]{
                        -1,
                        -1,
                        -1 / Math.sqrt(3),
                        1 / Math.sqrt(3),
                        1,
                        1,
                        1 / Math.sqrt(3),
                        -1 / Math.sqrt(3)};

                int it = 0;
                for (int i = 0; i < 4; i++) {
                    sides[i].ksi = new double[]{tempksi[it], tempksi[it + 1]};
                    sides[i].eta = new double[]{tempeta[it], tempeta[it + 1]};
                    sides[i].detJ = Math.sqrt(Math.pow(x[2] - x[3], 2) + Math.pow(y[0] - y[1], 2)) / 2;
                    it += 2;
                }
                break;
            case 3:
                w = new double[]{5.0 / 9.0, 8.0 / 9.0, 5.0 / 9.0};
                double p1 = -Math.sqrt(3.0 / 5.0);
                double p2 = 0.0;
                double p3 = Math.sqrt(3.0 / 5.0);
                ksi = new double[]{p1, p2, p3, p1, p2, p3, p1, p2, p3};
                eta = new double[]{p1, p1, p1, p2, p2, p2, p3, p3, p3};
                sides = null;
                break;
            case 4:
                w = new double[]{((18.0 - Math.sqrt(30)) / 36.0), (18.0 + Math.sqrt(30.0)) / 36.0, (18.0 + Math.sqrt(30.0)) / 36.0, (18.0 - Math.sqrt(30.0)) / 36.0};
                double p11 = Math.sqrt((3.0 / 7.0) - (2.0 / 7.0) * Math.sqrt(6.0 / 5.0));
                double p22 = Math.sqrt((3.0 / 7.0) + (2.0 / 7.0) * Math.sqrt(6.0 / 5.0));
                eta = new double[]{
                        -p22, -p22, -p22, -p22,
                        -p11, -p11, -p11, -p11,
                        p11, p11, p11, p11,
                        p22, p22, p22, p22
                };
                ksi = new double[]{
                        -p22, -p11, p11, p22,
                        -p22, -p11, p11, p22,
                        -p22, -p11, p11, p22,
                        -p22, -p11, p11, p22
                };
                sides = null;
                break;
            default:
                w = null;
                ksi = null;
                eta = null;
                sides = null;
        }
        detJacobians = new double[ksi.length];
        dNdKsi = new double[ksi.length][x.length];
        dNdEta = new double[ksi.length][x.length];
        N = new double[ksi.length][x.length];
        invertedJacobians = new double[ksi.length][x.length];
        evaluateNDerivatives();
        calculateN();
        evaluateInvertedJacobians(x, y);
        calculateHB();
    }

    private double[] calculateWeights() {
        double[] weights = new double[ksi.length];
        int it = 0;
        for (int i = 0; i < GlobalData.npc; i++) {
            for (int j = 0; j < GlobalData.npc; j++) {
                weights[it] = w[i] * w[j];
                it++;
            }
        }
        return weights;
    }

    private void evaluateNDerivatives() {
        //Obliczanie pochodnych funkcji kształtu
        for (int i = 0; i < ksi.length; i++) {
            dNdKsi[i][0] = -0.25 * (1.0 - eta[i]);
            dNdKsi[i][1] = 0.25 * (1.0 - eta[i]);
            dNdKsi[i][2] = 0.25 * (1.0 + eta[i]);
            dNdKsi[i][3] = -0.25 * (1.0 + eta[i]);

            dNdEta[i][0] = -0.25 * (1.0 - ksi[i]);
            dNdEta[i][1] = -0.25 * (1.0 + ksi[i]);
            dNdEta[i][2] = 0.25 * (1.0 + ksi[i]);
            dNdEta[i][3] = 0.25 * (1.0 - ksi[i]);
        }
    }

    private void calculateN() {
        // Obliczanie funkcji kształtu
        for (int i = 0; i < ksi.length; i++) {
            N[i][0] = 0.25 * (1.0 - ksi[i]) * (1 - eta[i]);
            N[i][1] = 0.25 * (1.0 + ksi[i]) * (1 - eta[i]);
            N[i][2] = 0.25 * (1.0 + ksi[i]) * (1 + eta[i]);
            N[i][3] = 0.25 * (1.0 - ksi[i]) * (1 + eta[i]);
        }

        for (int i = 0; i < sides.length; i++) {
            //funkcje ksztaltu - warunek brzegowy
            for (int j = 0; j < sides[i].ksi.length; j++) {
                sides[i].N[j][0] = 0.25 * (1.0 - sides[i].ksi[j]) * (1 - sides[i].eta[j]);
                sides[i].N[j][1] = 0.25 * (1.0 + sides[i].ksi[j]) * (1 - sides[i].eta[j]);
                sides[i].N[j][2] = 0.25 * (1.0 + sides[i].ksi[j]) * (1 + sides[i].eta[j]);
                sides[i].N[j][3] = 0.25 * (1.0 - sides[i].ksi[j]) * (1 + sides[i].eta[j]);
            }
        }
    }

    public void evaluateInvertedJacobians(double[] x, double[] y) {
        // Obliczanie jakobianu
        final double[][] jacobians = new double[ksi.length][4];
        double dXdKsi = 0, dXdEta = 0, dYdKsi = 0, dYdEta = 0;

        for (int i = 0; i < ksi.length; i++) {
            for (int j = 0; j < 4; j++) {
                dXdKsi += x[j] * dNdKsi[i][j];
                dYdKsi += y[j] * dNdKsi[i][j];
                dXdEta += x[j] * dNdEta[i][j];
                dYdEta += y[j] * dNdEta[i][j];
            }
            jacobians[i][0] = dXdKsi;
            jacobians[i][1] = dYdKsi;
            jacobians[i][2] = dXdEta;
            jacobians[i][3] = dYdEta;
            detJacobians[i] = (jacobians[i][0] * jacobians[i][3]) - (jacobians[i][1] * jacobians[i][2]);
            dXdKsi = 0;
            dXdEta = 0;
            dYdKsi = 0;
            dYdEta = 0;
        }

        // Odwracanie jakobianu
        for (int i = 0; i < ksi.length; i++) {
            invertedJacobians[i][0] = jacobians[i][3] * (1 / detJacobians[i]);
            invertedJacobians[i][1] = -jacobians[i][1] * (1 / detJacobians[i]);
            invertedJacobians[i][2] = -jacobians[i][2] * (1 / detJacobians[i]);
            invertedJacobians[i][3] = jacobians[i][0] * (1 / detJacobians[i]);
        }
    }

    public double[][] evaluateH() {
        double[][] H = new double[4][4];

        final double[] dNdx = new double[4];
        final double[] dNdy = new double[4];

        double[][] mat1 = new double[4][4];
        double[][] mat2 = new double[4][4];
        double[][] mat3 = new double[4][4];
        double[] weights = calculateWeights();

        for (int k = 0; k < ksi.length; k++) {

            for (int i = 0; i < 4; i++) {
                dNdx[i] = invertedJacobians[k][0] * dNdKsi[k][i] + invertedJacobians[k][1] * dNdEta[k][i];
                dNdy[i] = invertedJacobians[k][2] * dNdKsi[k][i] + invertedJacobians[k][3] * dNdEta[k][i];
            }

            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    mat1[i][j] = dNdx[i] * dNdx[j];
                    mat2[i][j] = dNdy[i] * dNdy[j];
                }
            }

            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    mat3[i][j] = mat1[i][j] + mat2[i][j];
                    mat3[i][j] *= GlobalData.K * detJacobians[k];
                }
            }

            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    H[i][j] += mat3[i][j] * weights[k];
                }
            }
        }
        return H;
    }

    public double[][] evaluateC() {
        double[][] C = new double[4][4];

        double[][] mat3 = new double[4][4];
        double[] weights = calculateWeights();

        for (int k = 0; k < ksi.length; k++) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    mat3[i][j] = N[k][i] * N[k][j];
                    mat3[i][j] *= detJacobians[k] * GlobalData.ro * GlobalData.c;
                }
            }

            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    C[i][j] += mat3[i][j] * weights[k];
                }
            }
        }
        return C;
    }

    public double[][] calculateHB() {
        double[][] HB = new double[4][4];
        double[][] NNt = new double[4][4]; // N razy N transponowane
        double[] weights = calculateWeights();

        for (int k = 0; k < sides.length; k++) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    NNt[i][j] = (sides[k].N[0][i] * sides[k].N[0][j]) + (sides[k].N[1][i] * sides[k].N[1][j]);
                }
            }

            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    HB[i][j] += NNt[i][j] * weights[k] * sides[k].detJ * 25;
                }
            }
        }
        return HB;
    }
}
