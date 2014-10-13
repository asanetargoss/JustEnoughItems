package mezz.jei;

import mezz.jei.util.HashMapCache;
import mezz.jei.util.Log;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

public class ItemFilter {
	private static final int cacheSize = 16;

	private String filterText = "";

	/**
	 * The filter map for when there is no filter. Maps itemStack names to itemStacks
 	 */
	private final LinkedHashMap<String, ItemStack> unfilteredItemMap = new LinkedHashMap<String, ItemStack>();

	/**
	 * The currently active filter map. Maps itemStack names to itemStacks
	 */
	private LinkedHashMap<String, ItemStack> filteredItemMap = unfilteredItemMap;

	/**
	 * A cache for fast searches while typing or using backspace. Maps filterText to filteredItemMaps
	 */
	private final HashMapCache<String, LinkedHashMap<String, ItemStack>> filteredItemMapsCache = new HashMapCache<String, LinkedHashMap<String, ItemStack>>(cacheSize);

	/**
	 * A cache for for fast getItemList(). Maps filterText to itemList
	 */
	private final HashMapCache<String, List<ItemStack>> itemListsCache = new HashMapCache<String, List<ItemStack>>(cacheSize);

	public ItemFilter(final ItemRegistry registry) {
		for (ItemStack itemStack : registry.itemList) {
			try {
				unfilteredItemMap.put(itemStack.getDisplayName().toLowerCase(), itemStack);
			} catch (RuntimeException e) {
				Log.warning("Found item with broken getDisplayName: " + itemStack);
			}
		}
		setFilterText(filterText);
	}

	public boolean setFilterText(String filterText) {
		filterText = filterText.toLowerCase();
		if (this.filterText.equals(filterText))
			return false;

		this.filteredItemMap = getFilteredItemMap(filterText);
		this.filterText = filterText;
		return true;
	}

	public String getFilterText() {
		return filterText;
	}

	private LinkedHashMap<String, ItemStack> getFilteredItemMap(String filterText) {
		if (filterText.isEmpty())
			return unfilteredItemMap;

		if (filteredItemMapsCache.containsKey(filterText))
			return filteredItemMapsCache.get(filterText);

		// Find a cached filter that is a superset of the one we want,
		// so we don't have to filter the full item list.
		// For example, the "ir" and "iro" filters are supersets of the "iron" filter.
		String longestExistingFilterText = "";
		for (String existingFilterText : filteredItemMapsCache.keySet()) {
			if (filterText.startsWith(existingFilterText) && (existingFilterText.length() > longestExistingFilterText.length())) {
				longestExistingFilterText = existingFilterText;
			}
		}

		LinkedHashMap<String, ItemStack> baseItemMap;
		if (longestExistingFilterText.equals("")) {
			baseItemMap = unfilteredItemMap;
		} else {
			baseItemMap = filteredItemMapsCache.get(longestExistingFilterText);
		}
		LinkedHashMap<String, ItemStack> filteredItemMap = new LinkedHashMap<String, ItemStack>(baseItemMap);

		for (Iterator<String> iterator = filteredItemMap.keySet().iterator(); iterator.hasNext();) {
			String key = iterator.next();
			if (!key.contains(filterText)) {
				iterator.remove();
			}
		}

		filteredItemMapsCache.put(filterText, filteredItemMap);

		return filteredItemMap;
	}

	public List<ItemStack> getItemList() {
		List<ItemStack> itemList = itemListsCache.get(filterText);
		if (itemList == null) {
			itemList = new ArrayList<ItemStack>(filteredItemMap.values());
			itemListsCache.put(filterText, itemList);
		}
		return itemList;
	}

	public int size() {
		return filteredItemMap.size();
	}

}