package com.demo.flyingcar;

import java.io.IOException;
import java.util.Locale;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Tiny JSON API. Tesla has NOT published thruster specs, so the physics model uses
 * adjustable assumptions: compressed air (gamma 1.4) expanding through a nozzle.
 */
@WebServlet("/api/*")
public class ApiServlet extends HttpServlet {

    static final double G = 9.80665, R_AIR = 287.0, GAMMA = 1.4, P_AMB = 101_325.0;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        String json = switch (path) {
            case "/physics" -> physics(req);
            case "/compare" -> compare();
            default -> "{\"error\":\"not found\"}";
        };
        resp.getWriter().write(json);
    }

    private static double num(HttpServletRequest r, String k, double def, double lo, double hi) {
        try { return Math.max(lo, Math.min(hi, Double.parseDouble(r.getParameter(k)))); }
        catch (Exception e) { return def; }
    }

    private String physics(HttpServletRequest r) {
        double dry = num(r, "mass", 1800, 800, 4000);     // kg, vehicle without gas
        double gas = num(r, "gas", 40, 5, 200);           // kg of stored gas
        double p0 = num(r, "pbar", 300, 50, 700) * 1e5;   // tank pressure, Pa
        double twr = num(r, "twr", 1.5, 1.05, 4);         // thrust-to-weight at liftoff
        double temp = 300;                                // K

        // Isentropic nozzle exit velocity: ve = sqrt(2*gamma/(gamma-1) * R*T * (1-(pe/p0)^((gamma-1)/gamma)))
        double ve = Math.sqrt(2 * GAMMA / (GAMMA - 1) * R_AIR * temp
                * (1 - Math.pow(P_AMB / p0, (GAMMA - 1) / GAMMA)));
        double isp = ve / G;
        double m0 = dry + gas;
        double thrust = twr * m0 * G;
        double mdot = thrust / ve;                        // F = mdot * ve
        double burn = gas / mdot;
        double dv = ve * Math.log(m0 / dry);              // Tsiolkovsky
        double hover = (ve / G) * Math.log(m0 / dry);     // seconds of pure hover

        StringBuilder traj = new StringBuilder();
        double dt = 0.001, t = 0, h = 0, v = 0, maxH = 0;
        int step = 0;
        while (t < 60 && !(t > 0.01 && h <= 0)) {
            if (t < burn) { double m = m0 - mdot * t; v += (thrust / m - G) * dt; }
            else v -= G * dt;
            h = Math.max(0, h + v * dt);
            maxH = Math.max(maxH, h);
            t += dt;
            if (step++ % 50 == 0) traj.append(String.format(Locale.ROOT, "[%.2f,%.2f],", t, h));
        }
        if (traj.length() > 0) traj.setLength(traj.length() - 1);

        return String.format(Locale.ROOT, """
            {"ve":%.1f,"isp":%.1f,"thrustKN":%.1f,"mdot":%.1f,"burn":%.2f,"hover":%.2f,\
            "deltaV":%.1f,"maxH":%.2f,"flight":%.2f,"traj":[%s]}""",
            ve, isp, thrust / 1000, mdot, burn, hover, dv, maxH, t, traj);
    }

    private String compare() {
        // Approximate, illustrative figures for teaching (not official specs).
        return """
            [
             {"name":"Tesla Roadster thrusters","kind":"Cold gas (compressed air)","isp":70,"sec":2,"note":"Short hops, flips. Demo was autonomous."},
             {"name":"Falcon 9 Merlin","kind":"Rocket, RP-1 / LOX","isp":311,"sec":160,"note":"Reaches orbit; reusable booster."},
             {"name":"Starship Raptor","kind":"Rocket, methane / LOX","isp":350,"sec":360,"note":"Full-flow staged combustion."},
             {"name":"Harrier jump jet","kind":"Vectored turbofan","isp":0,"sec":600,"note":"Real VTOL, burns jet fuel, uses air."},
             {"name":"Helicopter","kind":"Rotor (airfoil lift)","isp":0,"sec":10800,"note":"Efficient hover; needs air and big rotors."},
             {"name":"eVTOL air taxi","kind":"Electric rotors + wings","isp":0,"sec":2700,"note":"Quiet, certifiable path to market."}
            ]""";
    }
}
