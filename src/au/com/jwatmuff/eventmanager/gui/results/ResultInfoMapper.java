/*
 * EventManager
 * Copyright (c) 2008-2017 James Watmuff & Leonard Hall
 *
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.com.jwatmuff.eventmanager.gui.results;

import au.com.jwatmuff.eventmanager.model.info.ResultInfo;
import au.com.jwatmuff.eventmanager.model.vo.Player;
import au.com.jwatmuff.eventmanager.model.vo.Pool;
import au.com.jwatmuff.eventmanager.util.BeanMapper;
import au.com.jwatmuff.genericdb.Database;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author james
 */
public class ResultInfoMapper implements BeanMapper<ResultInfo> {
    private Database database;
    private NumberFormat format = new DecimalFormat();
    private DateFormat dformat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");

    public ResultInfoMapper(Database database) {
        this.database = database;
        format.setMinimumIntegerDigits(3);
    }
    
    @Override
    public Map<String, Object> mapBean(ResultInfo bean) {

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("matfight", bean.getMatName() + " " + format.format(bean.getMatFightNumber()));
        map.put("division", database.get(Pool.class, bean.getFight().getPoolID()).getDescription());
        for(int i = 0; i < 2; i++) {
            Player player = bean.getPlayer()[i];
            map.put("player" + (i + 1), bean.getPlayerName()[i]);
            map.put("playerId" + (i + 1), (player != null) ? player.getVisibleID() : "N/A");
        }

        double[] scores = bean.getResult().getSimpleScores(database);
        map.put("score", scores[0] + " : " + scores[1]);
        if(scores[0] > scores[1])
            map.put("winner", bean.getPlayerName()[0]);
        else if(scores[0] < scores[1])
            map.put("winner", bean.getPlayerName()[1]);
        else
            map.put("winner", "Draw");

        map.put("time", dformat.format(bean.getResult().getTimestamp()));
        map.put("timerec", bean.getResult().getTimestamp()); // for printing

        return map;
    }
}
