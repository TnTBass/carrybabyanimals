package dev.jasmine.carrybabyanimals.fabric.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.jasmine.carrybabyanimals.client.config.CarryBabyAnimalsConfigScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class CarryBabyAnimalsModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return CarryBabyAnimalsConfigScreen::new;
    }
}
