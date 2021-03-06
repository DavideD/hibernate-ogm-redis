/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.dialect.model.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.ogm.datastore.document.association.spi.AssociationRow.AssociationRowAccessor;
import org.hibernate.ogm.datastore.document.association.spi.AssociationRowFactory;
import org.hibernate.ogm.datastore.document.association.spi.StructureOptimizerAssociationRowFactory;
import org.hibernate.ogm.datastore.document.impl.DotPatternMapHelpers;

/**
 * {@link AssociationRowFactory} which creates association rows based on the map based representation used in Redis.
 *
 * @author Gunnar Morling
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Mark Paluch
 */
public class RedisAssociationRowFactory extends StructureOptimizerAssociationRowFactory<Map<String, Object>> {

	public static final RedisAssociationRowFactory INSTANCE = new RedisAssociationRowFactory();

	private RedisAssociationRowFactory() {
		super( Map.class );
	}

	@Override
	protected Map<String, Object> getSingleColumnRow(String columnName, Object value) {
		return Collections.singletonMap( columnName, value );
	}

	@Override
	protected AssociationRowAccessor<Map<String, Object>> getAssociationRowAccessor(
			String[] prefixedColumns,
			String prefix) {

		return prefix != null ?
				new RedisAssociationRowAccessor( prefixedColumns, prefix ) :
				RedisAssociationRowAccessor.INSTANCE;
	}

	private static class RedisAssociationRowAccessor implements AssociationRowAccessor<Map<String, Object>> {

		private static final RedisAssociationRowAccessor INSTANCE = new RedisAssociationRowAccessor();

		private final String prefix;
		private final List<String> prefixedColumns;

		public RedisAssociationRowAccessor() {
			this( null, null );
		}

		public RedisAssociationRowAccessor(String[] prefixedColumns, String prefix) {
			this.prefix = prefix;
			if ( prefix != null ) {
				this.prefixedColumns = Arrays.asList( prefixedColumns );
			}
			else {
				this.prefixedColumns = Collections.emptyList();
			}
		}

		// only call if you have a prefix
		private String unprefix(String prefixedColumn) {
			return prefixedColumn.substring( prefix.length() + 1 ); //name + "."
		}

		@Override
		public Set<String> getColumnNames(Map<String, Object> row) {
			Set<String> columnNames = new HashSet<>( row.keySet() );
			addColumnNames( row, columnNames, "" );
			for ( String prefixedColumn : prefixedColumns ) {
				String unprefixedColumn = unprefix( prefixedColumn );
				if ( columnNames.contains( unprefixedColumn ) ) {
					columnNames.remove( unprefixedColumn );
					columnNames.add( prefixedColumn );
				}
			}
			return columnNames;
		}

		private void addColumnNames(Map<String, Object> row, Set<String> columnNames, String prefix) {
			for ( String field : row.keySet() ) {
				Object sub = row.get( field );
				if ( sub instanceof Map ) {
					addColumnNames( (Map) sub, columnNames, DotPatternMapHelpers.flatten( prefix, field ) );
				}
				else {
					columnNames.add( DotPatternMapHelpers.flatten( prefix, field ) );
				}
			}
		}

		@Override
		public Object get(Map<String, Object> row, String column) {
			if ( prefixedColumns.contains( column ) ) {
				column = unprefix( column );
			}

			if ( row.containsKey( column ) ) {
				return row.get( column );
			}

			return DotPatternMapHelpers.getValueOrNull( row, column );
		}
	}
}
