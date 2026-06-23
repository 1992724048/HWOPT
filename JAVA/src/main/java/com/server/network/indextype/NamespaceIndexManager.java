package com.server.network.indextype;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class NamespaceIndexManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("NamespaceIndex");
    private static volatile boolean initialized = false;
    private static final List<String> NAMESPACES = new ArrayList<>();
    private static final List<List<String>> PATHS = new ArrayList<>();
    private static final Object2IntMap<String> NAMESPACE_MAP = new Object2IntOpenHashMap<>();
    private static final Int2ObjectArrayMap<Object2IntMap<String>> PATH_MAPS = new Int2ObjectArrayMap<>();
    private static volatile int payloadCount = 0;
    static { NAMESPACE_MAP.defaultReturnValue(-1); }

    private static final List<String> VANILLA_PATHS = List.of(
        "bundle","bundle_delimiter","add_entity","animate","award_stats","block_changed_ack",
        "block_destruction","block_entity_data","block_event","block_update","boss_event",
        "change_difficulty","chunk_batch_finished","chunk_batch_start","chunks_biomes","clear_titles",
        "command_suggestions","commands","container_close","container_set_content","container_set_data",
        "container_set_slot","cooldown","custom_chat_completions","damage_event","debug/block_value",
        "debug/chunk_value","debug/entity_value","debug/event","debug_sample","delete_chat",
        "disguised_chat","entity_event","entity_position_sync","explode","forget_level_chunk",
        "game_event","game_test_highlight_pos","mount_screen_open","hurt_animation","initialize_border",
        "level_chunk_with_light","level_event","level_particles","light_update","login",
        "low_disk_space_warning","map_item_data","merchant_offers","move_entity_pos","move_entity_pos_rot",
        "move_minecart_along_track","move_entity_rot","move_vehicle","open_book","open_screen",
        "open_sign_editor","place_ghost_recipe","player_abilities","player_chat","player_combat_end",
        "player_combat_enter","player_combat_kill","player_info_remove","player_info_update","player_look_at",
        "player_position","player_rotation","recipe_book_add","recipe_book_remove","recipe_book_settings",
        "remove_entities","remove_mob_effect","respawn","rotate_head","section_blocks_update",
        "select_advancements_tab","server_data","set_action_bar_text","set_border_center","set_border_lerp_size",
        "set_border_size","set_border_warning_delay","set_border_warning_distance","set_camera",
        "set_chunk_cache_center","set_chunk_cache_radius","set_default_spawn_position","set_display_objective",
        "set_entity_data","set_entity_link","set_entity_motion","set_equipment","set_experience","set_health",
        "set_held_slot","set_objective","set_passengers","set_player_team","set_score","set_simulation_distance",
        "set_subtitle_text","set_time","set_title_text","set_titles_animation","sound_entity","sound",
        "start_configuration","stop_sound","system_chat","tab_list","tag_query","take_item_entity",
        "teleport_entity","test_instance_block_status","update_advancements","update_attributes",
        "update_mob_effect","update_recipes","projectile_power","waypoint","accept_teleportation",
        "block_entity_tag_query","bundle_item_selected","change_game_mode","chat_ack","chat_command",
        "chat_command_signed","chat","chat_session_update","chunk_batch_received","client_command",
        "client_tick_end","command_suggestion","configuration_acknowledged","container_button_click",
        "container_click","container_slot_state_changed","debug_subscription_request","edit_book",
        "entity_tag_query","interact","jigsaw_generate","lock_difficulty","move_player_pos",
        "move_player_pos_rot","move_player_rot","move_player_status_only","paddle_boat",
        "pick_item_from_block","pick_item_from_entity","place_recipe","player_action","player_command",
        "player_input","player_loaded","recipe_book_change_settings","recipe_book_seen_recipe","rename_item",
        "seen_advancements","select_trade","set_beacon","set_carried_item","set_command_block",
        "set_command_minecart","set_creative_mode_slot","set_jigsaw_block","set_structure_block","set_test_block",
        "test_instance_block_action","sign_update","swing","teleport_to_entity","use_item_on","use_item",
        "reset_score","ticking_state","ticking_step","set_cursor_item","set_player_inventory");

    public static synchronized void init(List<Identifier> types) {
        initialized = false;
        NAMESPACES.clear(); PATHS.clear(); NAMESPACE_MAP.clear(); PATH_MAPS.clear();
        AtomicInteger nsIdx = new AtomicInteger(1);
        NAMESPACES.add("ILLEGAL"); PATHS.add(new ArrayList<>()); payloadCount = 0;
        VANILLA_PATHS.forEach(p -> fillSingle(nsIdx, Identifier.withDefaultNamespace(p)));
        if (types != null) {
            for (Identifier t : types) { fillSingle(nsIdx, t); payloadCount++; }
        }
        initialized = true;
        LOGGER.info("NSIndex: {} namespaces, {} payloads", NAMESPACES.size()-1, payloadCount);
    }

    public static int getPayloadCount() { return payloadCount; }

    private static void fillSingle(AtomicInteger nsIdx, Identifier id) {
        if (!NAMESPACE_MAP.containsKey(id.getNamespace())) {
            NAMESPACE_MAP.put(id.getNamespace(), nsIdx.get());
            NAMESPACES.add(id.getNamespace()); PATHS.add(new ArrayList<>()); nsIdx.getAndIncrement();
        }
        int idx = NAMESPACE_MAP.getInt(id.getNamespace());
        PATH_MAPS.computeIfAbsent(idx, k -> new Object2IntOpenHashMap<>()).put(id.getPath(), PATH_MAPS.get(idx).size());
        PATHS.get(idx).add(id.getPath());
    }

    public static boolean contains(Identifier type) {
        if (!initialized) return false;
        Object2IntMap<String> pm = PATH_MAPS.get(NAMESPACE_MAP.getInt(type.getNamespace()));
        return pm != null && pm.containsKey(type.getPath());
    }

    public static int[] getCheckedIndex(Identifier type) {
        int nsId = NAMESPACE_MAP.getInt(type.getNamespace());
        return new int[]{nsId, PATH_MAPS.get(nsId).getInt(type.getPath())};
    }

    public static Identifier getIdentifier(int nsIdx, int pIdx) {
        if (!initialized || nsIdx == 0) return null;
        return Identifier.fromNamespaceAndPath(NAMESPACES.get(nsIdx), PATHS.get(nsIdx).get(pIdx));
    }

    public static boolean ready() { return initialized; }
}
