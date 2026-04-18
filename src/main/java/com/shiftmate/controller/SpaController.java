package com.shiftmate.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards all non-API, non-asset URL paths to {@code index.html} so that
 * React Router can handle client-side navigation. Without this, a hard refresh
 * on a path such as {@code /employees} would return a 404 from Spring because
 * no server-side route matches it.
 */
@Controller
public class SpaController {

    /**
     * Forwards any path that does not contain a file extension (and therefore
     * is not a static asset request) to the React application shell.
     *
     * @return a servlet forward to {@code /index.html}
     */
    @RequestMapping(value = {"/", "/{path:[^\\.]*}", "/{path:[^\\.]*}/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
