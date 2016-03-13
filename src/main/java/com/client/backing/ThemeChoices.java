package com.client.backing;

import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: multi
 * Date: 27/10/15
 * Time: 05:44
 * To change this template use File | Settings | File Templates.
 */

@ManagedBean
public class ThemeChoices {

    public static final String DEFAULT_THEME = "aristo";

    public static final String[] possibleThemes =

            { "afterdark", "afternoon", "afterwork", "aristo",

                    "black-tie", "blitzer", "bluesky", "casablanca",

                    "cruze", "cupertino", "dark-hive", "delta", "dot-luv",

                    "eggplant", "excite-bike", "flick", "glass-x",

                    "home", "hot-sneaks", "humanity", "le-frog",

                    "midnight", "mint-choc", "overcast", "pepper-grinder",

                    "redmond", "rocket", "sam", "smoothness",

                    "south-street", "start", "sunny", "swanky-purse",

                    "trontastic", "twitter bootstrap", "ui-darkness",

                    "ui-lightness", "vader" };

    public String[] getThemes() {

        List<String> themes = new LinkedList<>();

        for(String theme: possibleThemes)
        {
            themes.add(theme);
        }

        String currentTheme = currentTheme();

        themes.remove(currentTheme);

        themes.add(0, currentTheme);

        return(themes.toArray(new String[themes.size()]));
    }


    public String currentTheme() {

        String theme = DEFAULT_THEME;

        ExternalContext externalContext =

                FacesContext.getCurrentInstance().getExternalContext();

        String param =

                externalContext.getInitParameter("primefaces.THEME");

        if (param != null) {

            theme = param;

        }

        return(theme);

    }
}