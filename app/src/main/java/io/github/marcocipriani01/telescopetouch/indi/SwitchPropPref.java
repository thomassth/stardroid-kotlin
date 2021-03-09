/*
 * Copyright 2020 Marco Cipriani (@marcocipriani01)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.marcocipriani01.telescopetouch.indi;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.indilib.i4j.Constants;
import org.indilib.i4j.client.INDIProperty;
import org.indilib.i4j.client.INDISwitchElement;
import org.indilib.i4j.client.INDISwitchProperty;

import java.util.List;

import io.github.marcocipriani01.telescopetouch.R;
import io.github.marcocipriani01.telescopetouch.TelescopeTouchApp;

import static io.github.marcocipriani01.telescopetouch.TelescopeTouchApp.connectionManager;

public class SwitchPropPref extends PropPref<INDISwitchElement> {

    public SwitchPropPref(Context context, INDIProperty<INDISwitchElement> prop) {
        super(context, prop);
    }

    /**
     * Create the summary rich-text string
     *
     * @return the summary
     */
    @Override
    protected Spannable createSummary() {
        List<INDISwitchElement> elements = prop.getElementsAsList();
        int count = elements.size();
        if (count > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            int[] starts = new int[count];
            int[] ends = new int[count];
            starts[0] = 0;
            int i;
            for (i = 0; i < count - 1; i++) {
                starts[i] = stringBuilder.length();
                stringBuilder.append(elements.get(i).getLabel()).append(", ");
                ends[i] = stringBuilder.length();
            }
            starts[i] = stringBuilder.length();
            stringBuilder.append(elements.get(i).getLabel());
            ends[i] = stringBuilder.length();
            Spannable summaryText = new SpannableString(stringBuilder.toString());
            StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
            for (i = 0; i < count; i++) {
                if (elements.get(i).getValue() == Constants.SwitchStatus.ON) {
                    summaryText.setSpan(boldSpan, starts[i], ends[i], 0);
                }
            }
            return summaryText;
        } else {
            return new SpannableString(getContext().getString(R.string.no_indi_elements));
        }
    }

    @Override
    protected void onClick() {
        Context context = getContext();
        if (!getSummary().toString().equals(context.getString(R.string.no_indi_elements))) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final List<INDISwitchElement> elements = prop.getElementsAsList();
            String[] elementsString = new String[elements.size()];
            final boolean[] elementsChecked = new boolean[elements.size()];
            int singleCheckedItem = 0;
            for (int i = 0; i < elements.size(); i++) {
                INDISwitchElement switchElement = elements.get(i);
                elementsString[i] = switchElement.getLabel();
                boolean b = switchElement.getValue() == Constants.SwitchStatus.ON;
                elementsChecked[i] = b;
                if (b) singleCheckedItem = i;
            }

            Constants.SwitchRules rule = ((INDISwitchProperty) prop).getRule();
            if (rule == Constants.SwitchRules.ANY_OF_MANY) {
                builder.setMultiChoiceItems(elementsString, elementsChecked,
                        (dialog, which, isChecked) -> elementsChecked[which] = isChecked);
            } else if ((rule == Constants.SwitchRules.ONE_OF_MANY) || (rule == Constants.SwitchRules.AT_MOST_ONE)) {
                builder.setSingleChoiceItems(elementsString, singleCheckedItem,
                        (dialog, which) -> {
                            for (int i = 0; i < elementsChecked.length; i++) {
                                elementsChecked[i] = (i == which);
                            }
                        });
            }
            builder.setTitle(prop.getLabel());

            if (prop.getPermission() != Constants.PropertyPermissions.RO) {
                builder.setPositiveButton(R.string.send_request, (dialog, id) -> {
                    try {
                        for (int i = 0; i < elements.size(); i++) {
                            elements.get(i).setDesiredValue(elementsChecked[i] ? Constants.SwitchStatus.ON : Constants.SwitchStatus.OFF);
                        }
                    } catch (Exception e) {
                        Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        TelescopeTouchApp.connectionManager.log(e);
                    }
                    connectionManager.updateProperties(prop);
                });
                builder.setNegativeButton(android.R.string.cancel, null);
            } else {
                builder.setNegativeButton(R.string.back_request, null);
            }
            builder.setIcon(R.drawable.edit).show();
        }
    }
}